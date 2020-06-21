import itertools
import json
import multiprocessing as mp
import pickle
from argparse import ArgumentParser
from collections import defaultdict

import tensorflow as tf

MAX_CONTEXTS = 0
MAX_INTERNAL_PATHS = 0
MAX_PATH_LENGTH = 0
MAX_RELATIVE_PATH_LENGTH = 0
MAX_EXAMPLES_IN_SHARD = 500000

def save_dictionaries(dataset_name, subtok_to_count, node_to_count, max_contexts, max_internal_paths,
                      max_path_length, max_path_width, max_relative_path_length, max_child_id):
    save_dict_file_path = '{}.dict.cg'.format(dataset_name)
    with open(save_dict_file_path, 'wb') as file:
        pickle.dump(subtok_to_count, file)
        pickle.dump(node_to_count, file)
        pickle.dump(max_contexts, file)
        pickle.dump(max_internal_paths, file)
        pickle.dump(max_path_length, file)
        pickle.dump(max_path_width, file)
        pickle.dump(max_relative_path_length, file)
        pickle.dump(max_child_id, file)
        print('Dictionaries saved to: {}'.format(save_dict_file_path))

def make_example_from_line(line):
    obj = json.loads(line)
    return make_example(obj)

def make_example_and_histograms(line):
    local_node_to_count = defaultdict(int)
    local_subtoken_to_count = defaultdict(int)
    local_total_paths, local_total_examples = 0, 0
    obj = json.loads(line)
    targets = obj['targets']
    is_token_flags = obj['is_token']
    paths = obj['head_paths'] + obj['relative_paths']

    tokens = set()
    for target_val, is_tok in zip(targets, is_token_flags):
        if is_tok == 1:
            if ',' in target_val:
                values = target_val.split(',')
                for v in values:
                    local_subtoken_to_count[v] += 1
            else:
                local_subtoken_to_count[target_val] += 1
            tokens.add(target_val)
        else:
            local_node_to_count[target_val] += 1

    for path in paths:
        for subtok in path['sources']:
            local_subtoken_to_count[subtok] += 1
        for node_with_child in path['nodes']:
            node_with_child = node_with_child['node']
            if node_with_child[0] in tokens:
                continue
            node = node_with_child[0].rstrip('_INV')
            local_node_to_count[node] += 1
            if not node.startswith('@'):
                local_node_to_count[node + '_INV'] += 1
        local_total_paths += 1

    local_total_examples += len(targets)
    ex = make_example(obj)
    return ex, local_node_to_count, local_subtoken_to_count, local_total_paths, local_total_examples


def make_example(obj):
    ex = tf.train.SequenceExample()
    ex.context.feature['num_targets'].int64_list.value.append(obj['num_targets'])
    ex.context.feature['num_nodes'].int64_list.value.append(obj['num_nodes'])
    ex.context.feature['data_num_contexts'].int64_list.value.append(MAX_CONTEXTS)
    ex.context.feature['max_internal_paths'].int64_list.value.append(MAX_INTERNAL_PATHS)
    ex.context.feature['head_target_child_id'].int64_list.value.append(obj['head_child_id'])
    ex.context.feature['linearized_tree'].bytes_list.value.append(obj['linearized_tree'].encode())
    ex.context.feature['filepath'].bytes_list.value.append(obj['filepath'].encode())
    ex.context.feature['line'].int64_list.value.append(obj['line'])

    targets = ex.feature_lists.feature_list['targets']
    is_token = ex.feature_lists.feature_list['is_token']
    target_child_id = ex.feature_lists.feature_list['target_child_id']
    relative_path_nodes = ex.feature_lists.feature_list['relative_path_nodes']
    relative_child_ids = ex.feature_lists.feature_list['relative_path_child_ids']
    internal_path_nodes = ex.feature_lists.feature_list['internal_path_nodes']
    internal_path_nodes_sources = ex.feature_lists.feature_list['internal_paths_sources']
    internal_path_nodes_child_ids = ex.feature_lists.feature_list['internal_paths_child_ids']
    head_paths_sources = ex.feature_lists.feature_list['head_paths_sources']
    head_paths_tokens = ex.feature_lists.feature_list['head_paths_tokens']
    head_paths_nodes = ex.feature_lists.feature_list['head_paths_nodes']
    head_paths_child_ids = ex.feature_lists.feature_list['head_paths_child_ids']
    head_root_nodes = ex.feature_lists.feature_list['head_root_nodes']
    head_root_child_ids = ex.feature_lists.feature_list['head_root_child_ids']

    for target in obj['targets']:
        targets.feature.add().bytes_list.value.append(target.encode())
    for is_tok in obj['is_token']:
        is_token.feature.add().int64_list.value.append(is_tok)
    for tgt_child in obj['target_child_id']:
        target_child_id.feature.add().int64_list.value.append(tgt_child)

    for rps in obj['relative_paths']:
        node_child_pairs = rps['nodes'][-MAX_RELATIVE_PATH_LENGTH:]
        relative_path_nodes.feature.add().bytes_list.value.extend(nc['node'][0].encode() for nc in node_child_pairs)
        relative_child_ids.feature.add().int64_list.value.extend(int(nc['node'][1]) for nc in node_child_pairs)
    for paths_for_target in obj['internal_paths']:
        for i, path in enumerate(paths_for_target):
            if i >= MAX_INTERNAL_PATHS:
                break
            internal_path_nodes_sources.feature.add().bytes_list.value.extend(subtok.encode() for subtok in path['sources'])
            nodes_with_childs = path['nodes'][-MAX_PATH_LENGTH:]
            node_child_pairs = [nc['node'] for nc in nodes_with_childs]
            internal_path_nodes.feature.add().bytes_list.value.extend(nc[0].encode() for nc in node_child_pairs)
            internal_path_nodes_child_ids.feature.add().int64_list.value.extend(int(nc[1]) for nc in node_child_pairs)
        for j in range(MAX_INTERNAL_PATHS - min(MAX_INTERNAL_PATHS, len(paths_for_target))):
            internal_path_nodes_sources.feature.add()
            internal_path_nodes.feature.add()
            internal_path_nodes_child_ids.feature.add()

    sorted_head_paths = sorted(obj['head_paths'], key=lambda x: len(x['nodes']))
    for head_path in sorted_head_paths[:MAX_CONTEXTS]:
        node_child_pairs = head_path['nodes'][:MAX_PATH_LENGTH]
        head_paths_sources.feature.add().bytes_list.value.extend(subtok.encode() for subtok in head_path['sources'])
        head_paths_tokens.feature.add().bytes_list.value.append(','.join(head_path['sources']).encode())
        head_paths_nodes.feature.add().bytes_list.value.extend(nc['node'][0].encode() for nc in node_child_pairs)
        head_paths_child_ids.feature.add().int64_list.value.extend(int(nc['node'][1]) for nc in node_child_pairs)

    for node in obj['head_root_path']['nodes'][-MAX_PATH_LENGTH:]:
        node = node['node']
        head_root_nodes.feature.add().bytes_list.value.append(node[0].encode())
        head_root_child_ids.feature.add().int64_list.value.append(node[1])

    return ex.SerializeToString()



def process_file(file_path, data_file_role, dataset_name, max_contexts, max_internal_paths, max_path_length,
                 max_relative_path_length, collect_histograms=False):
    # Currently we take max contexts both from this script and from the json. 
    # When moving to joint paths, we should pad here and take max_contexts from the arguments and not the json
    total_paths = 0
    total_examples = 0
    subtoken_to_count = defaultdict(int)
    node_to_count = defaultdict(int)
    global MAX_CONTEXTS, MAX_INTERNAL_PATHS, MAX_PATH_LENGTH, MAX_RELATIVE_PATH_LENGTH
    MAX_CONTEXTS = max_contexts
    MAX_INTERNAL_PATHS = max_internal_paths
    MAX_PATH_LENGTH = max_path_length
    MAX_RELATIVE_PATH_LENGTH = max_relative_path_length

    with open(file_path, 'r') as file:
        current_shard_number = 0
        writer = create_writer(current_shard_number, data_file_role, dataset_name)

        if collect_histograms:
            with mp.Pool(64) as pool:
                examples_with_histograms = pool.imap_unordered(make_example_and_histograms, file, chunksize=100)
                #examples_with_histograms = [make_example_and_histograms(line) for line in file]
                for i, (ex, local_node_to_count, local_subtoken_to_count, local_total_paths,
                        local_total_examples) in enumerate(examples_with_histograms):
                    for key, val in local_node_to_count.items():
                        node_to_count[key] += val
                    for key, val in local_subtoken_to_count.items():
                        subtoken_to_count[key] += val
                    total_paths += local_total_paths
                    total_examples += local_total_examples

                    if (i+1) % MAX_EXAMPLES_IN_SHARD == 0:
                        current_shard_number += 1
                        writer.close()
                        writer = create_writer(current_shard_number, data_file_role, dataset_name)
                    writer.write(ex)

        else:
            with mp.Pool(64) as pool:
                serialized_examples = pool.imap_unordered(make_example_from_line, file, chunksize=100)
                #serialized_examples = [make_example_from_line(line) for line in file]
                for i, ex in enumerate(serialized_examples):
                    if (i+1) % MAX_EXAMPLES_IN_SHARD == 0:
                        current_shard_number += 1
                        writer.close()
                        writer = create_writer(current_shard_number, data_file_role, dataset_name)
                    writer.write(ex)

    writer.close()

    print('File: ' + file_path)
    if collect_histograms:
        print('Average total contexts: ' + str(float(total_paths) / total_examples))
        print('Total examples: ' + str(total_examples))
    return total_examples, subtoken_to_count, node_to_count


def create_writer(current_shard_number, data_file_role, dataset_name):
    output_path = '{}.tfrecord.{}.{}.cg'.format(dataset_name, data_file_role, current_shard_number)
    writer = tf.io.TFRecordWriter(output_path, options=tf.io.TFRecordCompressionType.GZIP)
    return writer


if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-trd", "--train_data", dest="train_data_path",
                        help="path to training data file", required=True)
    parser.add_argument("-ted", "--test_data", dest="test_data_path",
                        help="path to test data file", required=True)
    parser.add_argument("-vd", "--val_data", dest="val_data_path",
                        help="path to validation data file", required=True)
    parser.add_argument("-mc", "--max_contexts", dest="max_contexts", default=200,
                        help="number of max contexts to keep", required=False)
    parser.add_argument("--max_internal_paths", dest="max_internal_paths",
                        help="number of max internal paths to keep", required=True)
    parser.add_argument("-mp", "--max_path_length", dest="max_path_length", default=12,
                        required=False)
    parser.add_argument("--max_relative_path_length", dest="max_relative_path_length", default=6,
                        required=False)
    parser.add_argument("-mw", "--max_path_width", dest="max_path_width", default=3,
                        required=False)
    parser.add_argument("--max_child_id", dest="max_child_id", default=5,
                        help="number of max nodes to keep", required=False)
    parser.add_argument("-svs", "--subtoken_vocab_size", dest="subtoken_vocab_size", default=186277,
                        help="Max number of source subtokens to keep in the vocabulary", required=False)
    parser.add_argument("-o", "--output_name", dest="output_name",
                        help="output name - the base name for the created dataset", metavar="FILE", required=True,
                        default='data')
    args = parser.parse_args()

    train_data_path = args.train_data_path
    test_data_path = args.test_data_path
    val_data_path = args.val_data_path

    num_examples, subtoken_to_count, node_to_count = process_file(file_path=train_data_path, data_file_role='train',
                                                                  dataset_name=args.output_name,
                                                                  max_contexts=int(args.max_contexts),
                                                                  max_internal_paths=int(args.max_internal_paths),
                                                                  max_path_length=int(args.max_path_length),
                                                                  max_relative_path_length=int(args.max_relative_path_length),
                                                                  collect_histograms=True)
    for data_file_path, data_role in zip([test_data_path, val_data_path], ['test', 'val']):
        process_file(file_path=data_file_path, data_file_role=data_role, dataset_name=args.output_name,
                     max_contexts=int(args.max_contexts), max_internal_paths=int(args.max_internal_paths), 
                     max_path_length=int(args.max_path_length), max_relative_path_length=int(args.max_relative_path_length),
                     collect_histograms=False)

    save_dictionaries(dataset_name=args.output_name, subtok_to_count=subtoken_to_count,
                      node_to_count=node_to_count,
                      max_contexts=int(args.max_contexts), max_internal_paths=int(args.max_internal_paths), 
                      max_path_length=int(args.max_path_length), max_path_width=int(args.max_path_width),
                      max_relative_path_length=int(args.max_relative_path_length),
                      max_child_id=int(args.max_child_id))
