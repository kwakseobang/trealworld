package trealworld.adapter.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import trealworld.application.port.in.LoginCommand;
import trealworld.application.port.in.LoginUseCase;

@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final LoginUseCase login;

	AuthController(LoginUseCase login) {
		this.login = login;
	}

	/** 로그인. (ADR-0005: POST /api/auth/login → 200) */
	@PostMapping("/login")
	LoginResponse login(@RequestBody LoginRequest request) {
		return LoginResponse.of(login.login(new LoginCommand(request.email(), request.password())));
	}

}
