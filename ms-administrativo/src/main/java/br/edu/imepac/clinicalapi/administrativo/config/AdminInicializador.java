package br.edu.imepac.clinicalapi.administrativo.config;

import br.edu.imepac.clinicalapi.administrativo.entities.Perfil;
import br.edu.imepac.clinicalapi.administrativo.entities.Usuario;
import br.edu.imepac.clinicalapi.administrativo.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante um administrador para o primeiro acesso: cria o usuário {@code seguranca.admin.login}
 * (ADMIN_LOGIN / ADMIN_PASSWORD) se ele ainda não existir. Não altera a senha de um admin já criado.
 */
@Component
public class AdminInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicializador.class);
    private static final String SENHA_PADRAO_DEV = "admin12345";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SegurancaProperties props;

    public AdminInicializador(UsuarioRepository repository, PasswordEncoder passwordEncoder, SegurancaProperties props) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        SegurancaProperties.Admin admin = props.admin();
        if (admin == null || admin.login() == null || admin.login().isBlank()
                || admin.senha() == null || admin.senha().isBlank()) {
            log.info("Administrador inicial não configurado (seguranca.admin.*)");
            return;
        }
        if (repository.existsByLogin(admin.login())) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setLogin(admin.login());
        usuario.setSenha(passwordEncoder.encode(admin.senha()));
        usuario.setPerfil(Perfil.ADMIN);
        usuario.setAtivo(true);
        repository.save(usuario);
        log.info("Administrador inicial '{}' criado", admin.login());
        if (SENHA_PADRAO_DEV.equals(admin.senha())) {
            log.warn("Administrador criado com a senha padrão de desenvolvimento: defina ADMIN_PASSWORD fora do ambiente local");
        }
    }
}
