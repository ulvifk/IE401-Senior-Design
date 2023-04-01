import numpy as np


class Individual:
    score: float
    genes: np.array

    def __init__(self, genes: np.array):
        self.genes = genes
        self.score = 0

    def mate(self, other, max_gene, min_gene):
        child_genes = []
        for i in range(len(self.genes)):
            prob = np.random.random()
            if prob < 0.45:
                child_genes.append(self.genes[i])
            elif prob < 0.90:
                child_genes.append(other.genes[i])
            else:
                child_genes.append(np.random.uniform(min_gene[i], max_gene[i]))

        return Individual(np.array(child_genes))

    def discrete_mate(self, other, max_gene, min_gene):
        child_genes = []
        for i in range(len(self.genes)):
            prob = np.random.random()
            if prob < 0.35:
                child_genes.append(self.genes[i])
            elif prob < 0.70:
                child_genes.append(other.genes[i])
            else:
                child_genes.append(np.random.randint(min_gene[i], max_gene[i]))

        return Individual(np.array(child_genes))
