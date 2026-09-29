CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome_completo VARCHAR(150) NOT NULL,
    data_nascimento DATE NOT NULL,
    email VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    aceite_termos_em DATETIME NOT NULL,
    exclusao_solicitada_em DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE decisoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    contexto TEXT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    etapa_atual TINYINT NOT NULL DEFAULT 1,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_decisoes_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_decisoes_etapa_atual CHECK (etapa_atual BETWEEN 1 AND 6),
    CONSTRAINT fk_decisoes_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    INDEX idx_decisoes_usuario_status_atualizacao (usuario_id, status, data_atualizacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE opcoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decisao_id BIGINT NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_opcoes_decisao_id FOREIGN KEY (decisao_id) REFERENCES decisoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE criterios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decisao_id BIGINT NOT NULL,
    nome VARCHAR(200) NOT NULL,
    descricao TEXT NULL,
    peso INT NOT NULL DEFAULT 1,
    personalizado BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT chk_criterios_peso CHECK (peso >= 1),
    CONSTRAINT fk_criterios_decisao_id FOREIGN KEY (decisao_id) REFERENCES decisoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE avaliacoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    criterio_id BIGINT NOT NULL,
    opcao_id BIGINT NOT NULL,
    valor INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_avaliacoes_valor CHECK (valor >= 1),
    CONSTRAINT uk_avaliacoes_criterio_opcao UNIQUE (criterio_id, opcao_id),
    CONSTRAINT fk_avaliacoes_criterio_id FOREIGN KEY (criterio_id) REFERENCES criterios (id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacoes_opcao_id FOREIGN KEY (opcao_id) REFERENCES opcoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ideias_brainstorming (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decisao_id BIGINT NOT NULL,
    conteudo TEXT NOT NULL,
    categoria VARCHAR(50) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ideias_brainstorming_decisao_id FOREIGN KEY (decisao_id) REFERENCES decisoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE itens_gut (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decisao_id BIGINT NOT NULL,
    descricao VARCHAR(200) NOT NULL,
    gravidade INT NOT NULL,
    urgencia INT NOT NULL,
    tendencia INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_itens_gut_decisao_id FOREIGN KEY (decisao_id) REFERENCES decisoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pros_contras (
    id BIGINT NOT NULL AUTO_INCREMENT,
    opcao_id BIGINT NOT NULL,
    tipo TINYINT NOT NULL,
    descricao VARCHAR(300) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_pros_contras_tipo CHECK (tipo IN (0, 1)),
    CONSTRAINT fk_pros_contras_opcao_id FOREIGN KEY (opcao_id) REFERENCES opcoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE reflexoes_101010 (
    id BIGINT NOT NULL AUTO_INCREMENT,
    opcao_id BIGINT NOT NULL,
    curto_prazo TEXT NULL,
    medio_prazo TEXT NULL,
    longo_prazo TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE (opcao_id),
    CONSTRAINT fk_reflexoes_101010_opcao_id FOREIGN KEY (opcao_id) REFERENCES opcoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inversoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    opcao_id BIGINT NOT NULL,
    perspectiva VARCHAR(100) NULL,
    reflexao TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inversoes_opcao_id FOREIGN KEY (opcao_id) REFERENCES opcoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE resultados (
    id BIGINT NOT NULL AUTO_INCREMENT,
    opcao_id BIGINT NOT NULL,
    pontuacao_total INT NULL,
    data_calculo DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE (opcao_id),
    CONSTRAINT fk_resultados_opcao_id FOREIGN KEY (opcao_id) REFERENCES opcoes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE tokens_recuperacao_senha (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expira_em DATETIME NOT NULL,
    usado_em DATETIME NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (token_hash),
    CONSTRAINT fk_tokens_recuperacao_senha_usuario_id FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
