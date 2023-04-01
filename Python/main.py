import pickle

import numpy as np
from tqdm import tqdm

from Python.genetic_algorithm.discrete_genetic_algorithm import DiscreteGeneticAlgorithm
from Python.heuristic.heuristic_v2 import HeuristicV2
from heuristic import Heuristic
from heuristic.data import Parameters
from particle_swarm.particle_swarm import ParticleSwarm
from genetic_algorithm import GeneticAlgorithm
position_max = np.array([30, 30, 30, 80, 3, 3, 3, 5, 30, 30, 30])
position_min = np.array([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])

def particle_swarm():
    for seed in [1]:
        for n in [10]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            particle_swarm = ParticleSwarm(100, 0.5, 0.3, 0.8, position_max, position_min, parameters)
            particle_swarm.optimize(1000, output_path, stats_path)

def genetic_algorithm():
    for seed in [1]:
        for n in [15]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            genetic = GeneticAlgorithm(300, position_max, position_min, parameters)
            genetic.optimize(10000, output_path, stats_path)

def heuristic():
    for seed in [0]:
        for n in [15]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            heuristic = Heuristic(parameters)
            heuristic.optimize()
            heuristic.write_solution(parameters.scenario, output_path)
            heuristic.write_stats(stats_path)

def discrete_genetic_algorithm():
    for seed in [0]:
        for n in [15]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            genetic = DiscreteGeneticAlgorithm(100, position_max, position_min, parameters)
            genetic.optimize(10000, output_path, stats_path)

if __name__ == "__main__":
    genetic_algorithm()
