package br.com.bussola;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de integração com o contexto completo da aplicação e o MySQL real.
 * <p>
 * Sobe todos os beans, conecta ao banco configurado em {@code application.properties}
 * ({@code DB_URL}, padrão {@code jdbc:mysql://localhost:3306/bussola}; {@code DB_USERNAME},
 * padrão {@code root}; {@code DB_PASSWORD}), aplica as migrations pendentes do Flyway e
 * deixa o Hibernate validar as entidades contra as tabelas ({@code ddl-auto=validate}).
 * <p>
 * Só roda quando {@code DB_PASSWORD} e {@code JWT_SECRET} estão definidas no terminal que
 * executa o Maven; sem elas, aparece como <em>Skipped</em> no resumo do Maven. Nenhuma
 * credencial fica no repositório. Veja "Executar testes sem IntelliJ" no {@code COMO_RODAR.md}.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+",
        disabledReason = "Defina DB_PASSWORD (e o MySQL ligado) para rodar o teste de integração com o banco.")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+",
        disabledReason = "Defina JWT_SECRET para rodar o teste de integração com o contexto completo.")
class BussolaApplicationTests {

	@Test
	void contextLoads() {
	}

}
