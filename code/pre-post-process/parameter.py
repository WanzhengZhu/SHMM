import yaml


class yaml_loader:
    def __init__(self):
        pass

    def load(self, para_file):
        yaml.add_constructor('!join', self._concat)
        fin = open(para_file, 'r')
        return yaml.load(fin)

    def _concat(self, loader, node):
        seq = loader.construct_sequence(node)
        return ''.join([str(i) for i in seq])


if __name__ == '__main__':
    y = yaml_loader()
    para_file = './ny9m.yaml'
    para = y.load(para_file)
    print para
