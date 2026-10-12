-- Fixture MANUAL, apenas para o banco local de desenvolvimento.
-- Simula uma decisão que já concluiu o brainstorming. Não é migration.
-- Troque apenas o e-mail abaixo pelo de uma conta de teste cadastrada no Bússola.
-- Não altera decisões existentes. Cada execução válida cria uma nova decisão de teste.
USE bussola;
SET @email_teste_opcoes = 'SEU_EMAIL_DE_TESTE';

INSERT INTO decisoes (usuario_id, titulo, contexto, status, etapa_atual)
SELECT id, '[TESTE] Aceitar estágio ou focar na faculdade?',
       'Decisão de teste criada diretamente na etapa de opções enquanto a integração do brainstorming está pendente.',
       0, 3
FROM usuarios WHERE email = @email_teste_opcoes;

SET @id_teste_opcoes = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
SELECT @id_teste_opcoes AS decisao_id,
       IF(@id_teste_opcoes IS NULL, 'Nenhuma decisão criada: confira o e-mail da conta de teste.',
          CONCAT('http://127.0.0.1:8765/pages/opcoes.html?decisaoId=', @id_teste_opcoes)) AS resultado;
