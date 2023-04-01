import multiprocessing
import random

import numpy as np
from tqdm import tqdm

from Python.genetic_algorithm.individual import Individual
from Python.heuristic import Heuristic
from Python.heuristic.data import Parameters
from Python.heuristic.heuristicv3 import HeuristicV3
from Python.heuristic.stats import *

from Python.heuristic.heuristic_v2 import HeuristicV2


class GeneticAlgorithm:
    population: list
    best_individual: Individual
    _population_size: int
    _max_gene: np.array
    _min_gene: np.array

    _heuristic: Heuristic | HeuristicV2 | HeuristicV3
    _parameters: Parameters

    def __init__(self, population_size: int, max_gene: np.array, min_gene: np.array,
                 parameters: Parameters):
        self.population = []
        self._population_size = population_size
        self._max_gene = max_gene
        self._min_gene = min_gene
        self._parameters = parameters
        self._heuristic = Heuristic(parameters)

        self._initialize_population()

    def optimize(self, n_iteration, output_path, stats_path):
        self.best_individual = self.population[0]

        pbar = tqdm(range(n_iteration), desc="Genetic algorithm", postfix=f"Best score: {self.best_individual.score}")
        for i in pbar:
            self.population = sorted(self.population, key=lambda x: x.score)
            if self.population[0].score < self.best_individual.score:
                self.best_individual = self.population[0]

                self._score(self.best_individual)
                solutions = list(self._heuristic.solutions.values())
                self._heuristic.write_solution(self._parameters.scenario, output_path)
                self._heuristic.write_stats(stats_path)

                pbar.set_postfix({f"Best score": self.best_individual.score})

            new_generation = []
            s = int(0.1 * self._population_size)
            new_generation.extend(self.population[:s])

            s = int(0.9 * self._population_size)
            for _ in range(s):
                new_generation.append(self._generate_child(_))

            self.population = new_generation

    def _generate_child(self, s):
        parent1: Individual = random.choice(self.population[:50])
        parent2: Individual = random.choice(self.population[:50])
        child = parent1.mate(parent2, self._max_gene, self._min_gene)
        child.score = self._score(child)
        return child

    def _initialize_population(self):
        for i in range(self._population_size):
            genes = np.random.uniform(self._min_gene, self._max_gene)
            individual = Individual(genes)
            individual.score = self._score(individual)
            self.population.append(individual)

    def _score(self, individual: Individual) -> float:
        self._heuristic = Heuristic(self._parameters)
        self._heuristic.alpha_tardiness = individual.genes[0]
        self._heuristic.alpha_deviation = individual.genes[1]
        self._heuristic.alpha_completion_time = individual.genes[2]
        self._heuristic.tightness_window = individual.genes[3]
        self._heuristic.tardiness_power = individual.genes[4]
        self._heuristic.deviation_power = individual.genes[5]
        self._heuristic.time_power = individual.genes[6]
        self._heuristic.slack = individual.genes[7]
        self._heuristic.high_priority = individual.genes[8]
        self._heuristic.medium_priority = individual.genes[9]
        self._heuristic.low_priority = individual.genes[10]

        self._heuristic.optimize()
        objective = self._heuristic.get_objective_value()
        return objective
