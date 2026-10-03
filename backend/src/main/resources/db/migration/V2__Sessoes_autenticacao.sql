CREATE TABLE sessoes_autenticacao (
    id VARCHAR(36) NOT NULL,
    usuario_id BIGINT NOT NULL,
    ultima_atividade DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sessoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    INDEX idx_sessoes_ultima_atividade (ultima_atividade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
