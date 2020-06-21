import json
import multiprocessing as mp
import re
from argparse import ArgumentParser
from enum import Enum, auto
import javalang
from functools import partial

PRED_TOKEN = 'PRED'
modifiers = ['public', 'private', 'protected', 'static']

class TargetType(Enum):
    seq = auto()
    tree = auto()

    @staticmethod
    def from_string(s):
        try:
            return TargetType[s]
        except KeyError:
            raise ValueError()

target_type = TargetType.seq


RE_WORDS = re.compile(r'''
    # Find words in a string. Order matters!
    [A-Z]+(?=[A-Z][a-z]) |  # All upper case before a capitalized word
    [A-Z]?[a-z]+ |  # Capitalized words / all lower case
    [A-Z]+ |  # All upper case
    \d+ | # Numbers
    _ |
    \" |
    .+
''', re.VERBOSE)

TREE_SPLIT = re.compile(r'([(),])')

def split_subtokens(str):
    return [subtok for subtok in RE_WORDS.findall(str) if not subtok == '_']

def subtokenize(s):
    failed = False
    try:
        tokens = list(javalang.tokenizer.tokenize(s))
    except:
        try:
            tokens = list(javalang.tokenizer.tokenize(s + '()'))[:-2]
        except:
            try:
                tokens = list(javalang.tokenizer.tokenize('(' + s + ')'))[1:-1]
            except:
                tokens = s.split()
                failed = True

    if failed:
        return [' _ '.join(split_subtokens(i)) for i in tokens if not i in modifiers]
    else:
        return [' _ '.join(split_subtokens(i.value)) for i in tokens if not i.value in modifiers]

def subtokenize_tree(s):
    return ' '.join([sub for sub in re.split(TREE_SPLIT, s) if len(sub) > 0])

def process_line(target_type, max_targets, max_nodes, line):
    obj = json.loads(line)
    left_context = obj['left_context']
    right_context = obj['right_context']
    target_seq = obj['target_seq']
    num_targets = obj['num_targets']
    num_nodes = obj['num_nodes']

    if max_targets is not None and num_targets > max_targets:
        return None, None
    if max_nodes is not None and num_nodes > max_nodes:
        return None, None

    if target_type is TargetType.seq:
        target_pred = ' '.join(subtokenize(target_seq)).lower()
    elif target_type is TargetType.tree:
        target_pred = subtokenize_tree(obj['linearized_tree'])

    source = '{} {} {}'.format(' '.join(subtokenize(left_context)[-200:]).lower(), PRED_TOKEN, ' '.join(subtokenize(right_context)[:200]).lower())
    return source, target_pred

def process_file(file_path, data_file_role, dataset_name, target_type, max_targets, max_nodes):
    total_examples = 0

    source_output_path = '{}.{}.{}.source.txt'.format(dataset_name, target_type, data_file_role)
    target_output_path = '{}.{}.{}.target.txt'.format(dataset_name, target_type, data_file_role)
    with open(source_output_path, 'w') as source_output_file:
        with open(target_output_path, 'w') as target_output_file:
            with open(file_path, 'r') as file:
                subtokenize_line = partial(process_line, target_type, max_targets, max_nodes)
                with mp.Pool(64) as pool:
                    if data_file_role in ['test', 'val']:
                        examples = [process_line(target_type, max_targets, max_nodes, line) for line in file]
                    else:
                        examples = pool.imap_unordered(subtokenize_line, file, chunksize=100)
                    #examples = [process_line(target_type, max_targets, max_nodes, line) for line in file]
                    for source_seq, target_seq in examples:
                        if source_seq is None or target_seq is None:
                            continue
                        source_output_file.write(source_seq + '\n')
                        target_output_file.write(target_seq + '\n')
                        total_examples += 1
                        #print(source_seq, target_seq)


    print('File: ' + file_path)
    print('Total examples: ' + str(total_examples))

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-trd", "--train_data", dest="train_data_path",
                        help="path to training data file", required=True)
    parser.add_argument("-ted", "--test_data", dest="test_data_path",
                        help="path to test data file", required=True)
    parser.add_argument("-vd", "--val_data", dest="val_data_path",
                        help="path to validation data file", required=True)
    parser.add_argument("-o", "--output_name", dest="output_name",
                        help="output name - the base name for the created dataset", metavar="FILE", required=True,
                        default='data')
    parser.add_argument("--target_type", dest="target_type", type=TargetType.from_string, choices=list(TargetType), required=True)
    parser.add_argument("--max_targets", dest="max_targets", type=int, required=False, default=40)
    parser.add_argument("--max_nodes", dest="max_nodes", type=int, required=False, default=None)

    parser.add_argument('--local', action='store_true')
    args = parser.parse_args()

    train_data_path = args.train_data_path
    test_data_path = args.test_data_path
    val_data_path = args.val_data_path

    for data_file_path, data_role in zip([train_data_path, test_data_path, val_data_path], ['train', 'test', 'val']):
        process_file(file_path=data_file_path, data_file_role=data_role, dataset_name=args.output_name,
                     target_type=args.target_type, max_targets=args.max_targets, max_nodes=args.max_nodes)
