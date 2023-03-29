import numpy as np

from heuristic import Heuristic
from heuristic.data import Parameters
from particle_swarm.particle_swarm import ParticleSwarm
from genetic_algorithm import GeneticAlgorithm


def particle_swarm():
    for seed in [1]:
        for n in [10]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            position_max = np.array([20, 20, 20, 80, 2, 2, 2, 20, 20, 20, 20])
            position_min = np.array([0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0])

            particle_swarm = ParticleSwarm(100, 0.8, 0.1, 0.1, position_max, position_min, parameters)
            particle_swarm.optimize(1000, output_path, stats_path)

def brute():
    input_path = input_path = f"../Java/input/scenario_{1}_{10}.json"
    parameters = Parameters()
    parameters.read_data(input_path)

    best_score = float('inf')
    best_position = None

    for i1 in range(2, 21):
        for i2 in range(2, 21):
            for i3 in range(2, 21):
                for i4 in range(0, 81):
                    for i5 in range(0, 4):
                        for i6 in range(0, 4):
                            for i7 in range(0, 4):
                                for i8 in range(2, 101):
                                    heuristic = Heuristic(parameters)
                                    heuristic.alpha_tardiness = i1
                                    heuristic.alpha_deviation = i2
                                    heuristic.alpha_completion_time = i3
                                    heuristic.tightness_window = i4
                                    heuristic.tardiness_power = i5
                                    heuristic.deviation_power = i6
                                    heuristic.time_pow = i7
                                    heuristic.slack = i8

                                    heuristic.optimize()

                                    score = heuristic.get_objective_value()
                                    if score < best_score:
                                        best_score = score
                                        best_position = [i1, i2, i3, i4, i5, i6, i7, i8]
                                        print(f"New best score: {best_score}")
                                        print(f"New best position: {best_position}")

def genetic_algorithm():
    for seed in [0]:
        for n in [10]:
            input_path = f"../Java/input/scenario_{seed}_{n}.json"
            output_path = f"solution_{seed}_{n}.json"
            stats_path = f"stats_{seed}_{n}.json"

            parameters = Parameters()
            parameters.read_data(input_path)

            position_max = np.array([20, 20, 20, 80, 2, 2, 2, 5, 30, 30, 30])
            position_min = np.array([-20, -20, -20, 0, 0, 0, 0, 0, 0, 0, 0])

            genetic = GeneticAlgorithm(400, position_max, position_min, parameters)
            genetic.optimize(1000, output_path, stats_path)

if __name__ == "__main__":
    genetic_algorithm()