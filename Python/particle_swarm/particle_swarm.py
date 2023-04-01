import numpy as np

from Python.heuristic.data import Parameters
from .particle import Particle
from tqdm import tqdm
from matplotlib import pyplot as plt

from Python.heuristic import Heuristic
from Python.heuristic.heuristic_v2 import HeuristicV2


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
    _heuristic: Heuristic | HeuristicV2

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

        pbar = tqdm(range(n_iteration), desc="Particle swarm", postfix=f"Best score: {self.best_score}")
        for n in pbar:
            for particle in self.particles:
                velocity = self.inertia * particle.velocity + self.c1 * np.random.uniform(0, 1) * (
                        particle.best_position - particle.position) + self.c2 * np.random.uniform(0, 1) * (
                                   self.best_position - particle.position)

                particle.velocity = velocity
                for i in range(len(particle.velocity)):
                    if particle.velocity[i] + particle.position[i] > self._max_position[i] or \
                            particle.velocity[i] + particle.position[i] < self._min_position[i]:
                        particle.velocity[i] = -particle.velocity[i] / 2

                particle.position = particle.position + velocity

                score = self._score(particle)
                if score < particle.best_score:
                    particle.best_score = score
                    particle.best_position = particle.position
                if score < self.best_score:
                    self.best_score = score
                    self.best_position = particle.position

                    self._heuristic.write_solution(self._parameters.scenario, output_path)
                    self._heuristic.write_stats(stats_path)

                    pbar.set_postfix({"Best score": self.best_score,
                                     "Found at iteration": n})

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
        self._heuristic.reset()
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
