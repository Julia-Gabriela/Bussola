-- Fixture MANUAL: cria somente uma decisão fictícia para uma conta de teste.
-- Não é migration, não altera decisões existentes e não libera etapas no fluxo normal.
-- Execute no banco LOCAL depois de iniciar o backend atualizado (migration V4).
USE bussola;
SET @email_teste_101010 = 'SEU_EMAIL_DE_TESTE';
START TRANSACTION;
INSERT INTO decisoes (usuario_id, titulo, contexto, status, etapa_atual)
SELECT id, '[TESTE] Aceitar estágio ou focar na faculdade?',
       'Cenário isolado de teste da Análise; não representa conclusão real das etapas anteriores.', 0, 6
FROM usuarios WHERE email = @email_teste_101010;
SET @id_teste_101010 = IF(ROW_COUNT() = 1, LAST_INSERT_ID(), NULL);
INSERT INTO opcoes (decisao_id, nome, descricao)
SELECT @id_teste_101010, 'Aceitar o estágio', 'Conciliar trabalho e estudos.' WHERE @id_teste_101010 IS NOT NULL;
INSERT INTO opcoes (decisao_id, nome, descricao)
SELECT @id_teste_101010, 'Focar na faculdade', 'Dedicar mais tempo às disciplinas.' WHERE @id_teste_101010 IS NOT NULL;
COMMIT;
SELECT @id_teste_101010 AS decisao_id,
       IF(@id_teste_101010 IS NULL, 'Nenhuma decisão criada: confira o e-mail da conta de teste.',
          CONCAT('http://127.0.0.1:8765/pages/metodo-101010.html?decisaoId=', @id_teste_101010)) AS resultado;
