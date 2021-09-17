import json
import multiprocessing
import pickle
from argparse import ArgumentParser

def collect_stats(line):
    local_terminal_types = set()
    local_nonterminal_types = set()
    local_possible_children = {}
    obj = json.loads(line)
    relative_paths = obj['relative_paths']
    head_root_path = obj['head_root_path']
    tokens = [target for target, is_token in zip(obj['targets'], obj['is_token']) if is_token == 1]

    for i, path in enumerate(relative_paths):
        prev_node = None
        for node in head_root_path['nodes'] + path['nodes']:
            node = node['node']
            type = node[0]
            if type in tokens and prev_node[0] not in tokens:
                local_terminal_types.add(prev_node[0])
            elif type not in tokens and prev_node is not None:
                local_nonterminal_types.add(prev_node[0])
                cur_possible_children_for_parent = local_possible_children.get(prev_node[0], set())
                cur_possible_children_for_parent.add(type)
                local_possible_children[prev_node[0]] = cur_possible_children_for_parent
            prev_node = node
        if obj['is_token'][i] == 1 and prev_node[0] not in tokens:
            local_terminal_types.add(prev_node[0])
    return local_terminal_types, local_nonterminal_types, local_possible_children

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("--json", dest="json", required=True)
    parser.add_argument("--output", dest="output", required=False)
    args = parser.parse_args()
    
    terminal_types = set()
    nonterminal_types = set()
    possible_children = {}
    
    with open(args.json, 'r') as file:
        with multiprocessing.Pool(64) as pool:
            local_results = pool.imap_unordered(collect_stats, file, chunksize=100)
            #local_results = [collect_stats(line) for line in file]
            for local_terminal_types, local_nonterminal_types, local_possible_children in local_results:
                terminal_types = terminal_types.union(local_terminal_types)
                nonterminal_types = nonterminal_types.union(local_nonterminal_types)
                for key, val in local_possible_children.items():
                    cur_possible_children_for_parent = possible_children.get(key, set())
                    cur_possible_children_for_parent = cur_possible_children_for_parent.union(val)
                    possible_children[key] = cur_possible_children_for_parent
            

    terminal_and_nonterminal_nodes = terminal_types.intersection(nonterminal_types)
    if len(terminal_and_nonterminal_nodes) == 0:
        print('No sets that are both terminals and nonterminals were found (this is a good sign).')
    else:
        print('WARNING: the following nodes were found to be both terminals and nonterminals.')
        print('This is not necessarily a problem, but might be more difficult to generate trees: ')
        for n in terminal_and_nonterminal_nodes:
            print('\t', n)

    if args.output:
        with open(args.output, 'wb') as file:
            pickle.dump(terminal_types, file)
            pickle.dump(nonterminal_types, file)
            pickle.dump(possible_children, file)
    else:
        print('Terminal types: ', terminal_types)
        print('Nonterminal types: ', nonterminal_types)
        print('Max child id for node: ')
        print('Possible child nodes: ')
        for node, children in possible_children.items():
            print('{}: {}'.format(node, children))
    
    