import numpy as np

class Particle:
    id: int
    position: np.array
    velocity: np.array
    best_position: np.array
    best_score: float

    def __init__(self, id: int, position: np.array, best_score: float):
        self.id = id
        self.position = position
        self.velocity = np.zeros(position.shape)
        self.best_position = position
        self.best_score = best_score
