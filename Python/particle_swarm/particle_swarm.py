import numpy as np

from heuristic import *
from heuristic.data import Parameters
from .particle import Particle
from tqdm import tqdm
from matplotlib import pyplot as plt


class ParticleSwarm:
    particles: list[Particle]
    best_score: float
    best_position: np.array
    inertia: float
    c1: float
    c2: float
    _max_position: np.array
    _min_position: np.array

    _parameters: Parameters
    _heuristic: Heuristic

    def __init__(self, n_particle: int, inertia: float, c1: float, c2: float, position_max: np.array,
                 position_min: np.array,
                 parameters: Parameters):
        self.particles = []
        self.inertia = inertia
        self.c1 = c1
        self.c2 = c2
        self.best_score = float('inf')
        self.best_position = np.random.uniform(position_min, position_max)
        self._parameters = parameters
        self._heuristic = Heuristic(parameters)
        self._max_position = position_max
        self._min_position = position_min

        self._initialize_particles(n_particle, position_max, position_min)

    def optimize(self, n_iteration: int, output_path: str, stats_path: str):
        for i in tqdm(range(n_iteration)):
            for particle in self.particles:
                velocity = self.inertia * particle.velocity + self.c1 * np.random.uniform(0, 1) * (
                        particle.best_position - particle.position) + self.c2 * np.random.uniform(0, 1) * (
                                   self.best_position - particle.position)

                particle.velocity = velocity
                particle.position = particle.position + velocity
                particle.position = np.maximum(particle.position, self._min_position)
                particle.position = np.minimum(particle.position, self._max_position)

                score = self._score(particle)
                if score < particle.best_score:
                    particle.best_score = score
                    particle.best_position = particle.position
                if score < self.best_score:
                    self.best_score = score
                    self.best_position = particle.position

                    self._heuristic.write_solution(self._parameters.scenario, output_path)
                    self._heuristic.write_stats(stats_path)

                    print(f"New best score: {self.best_score}")
                    print(f"New best position: {self.best_position}")

            if i % 1 == 0:
                self._add_particle()

    def _add_particle(self):
        position = np.random.uniform(self._min_position, self._max_position)
        particle = Particle(len(self.particles), position, float('inf'))
        self.particles.append(particle)

    def plot_particles(self):
        x = [p.position[0] for p in self.particles]
        y = [p.position[2] for p in self.particles]
        plt.scatter(x, y)
        plt.scatter(self.best_position[0], self.best_position[2], color='red')
        plt.show()

    def _score(self, particle: Particle) -> float:
        self._heuristic = Heuristic(self._parameters)
        self._heuristic.alpha_tardiness = particle.position[0]
        self._heuristic.alpha_deviation = particle.position[1]
        self._heuristic.alpha_completion_time = particle.position[2]
        self._heuristic.tightness_window = particle.position[3]
        self._heuristic.tardiness_power = particle.position[4]
        self._heuristic.deviation_power = particle.position[5]
        self._heuristic.time_pow = particle.position[6]
        self._heuristic.slack = particle.position[7]
        self._heuristic.high_priority = particle.position[8]
        self._heuristic.medium_priority = particle.position[9]
        self._heuristic.low_priority = particle.position[10]

        self._heuristic.optimize()
        return self._heuristic.get_objective_value()

    def _initialize_particles(self, n_particle: int, position_max: np.array, position_min: np.array):
        for i in range(n_particle):
            position = np.random.uniform(position_min, position_max)
            particle = Particle(i, position, float('inf'))
            self.particles.append(particle)
