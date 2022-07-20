#!/usr/bin/env python3

import sys

import yaml


RESERVED_CHARACTERS = set('#$%&{}_~^\\')
PUNCTUATION = set('.,(){}[]$')


def chomp_ext(filename):
    pos = filename.rfind('.')
    return filename if pos < 0 else filename[:pos]


def as_node_idx(hashcode):
    return 'node_' + str(hashcode).replace('-', '_')


def escape(ch):
    return '\\' + ch + '{}' if ch in RESERVED_CHARACTERS else ch


def insert_zero_width_spaces(text):
    return ''.join(
        [escape(c) + '\\hspace{0pt}' if c in PUNCTUATION else escape(c) for c in
         text])


def print_task(task, **kwargs):
    node_idx = as_node_idx(task['hash'])
    label = insert_zero_width_spaces(task['taskType'])
    print(f'    {node_idx} [texlbl="{label}"]', **kwargs)

    for dep in task['dependencies']:
        dep_idx = as_node_idx(dep)
        print(f'    {dep_idx} -> {node_idx}', **kwargs)


def print_prologue(**kwargs):
    print('''digraph D {
    d2toptions = "--autosize"
    ratio = "compress"
    node [lblstyle="text width=10em,align=center"]
''', **kwargs)


def print_epilogue(**kwargs):
    print('}', **kwargs)


def main():
    if len(sys.argv) != 2:
        print(f'Usage: {sys.argv[0]} <filename>')
        sys.exit(1)

    input_fname = sys.argv[1]
    output_fname = chomp_ext(input_fname) + '.dot'

    with open(input_fname, 'r') as f:
        data = list(yaml.load_all(f, Loader=yaml.CLoader))

    with open(output_fname, 'w') as f:
        print_prologue(file=f)
        for task in data:
            print_task(task, file=f)
        print_epilogue(file=f)


if __name__ == '__main__':
    main()
