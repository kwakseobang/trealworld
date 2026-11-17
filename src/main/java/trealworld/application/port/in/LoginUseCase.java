package trealworld.application.port.in;

/**
 * 로그인: 회원이 이메일과 비밀번호로 자기를 증명한다. (Spec 01의 목표 2)
 */
public interface LoginUseCase {

	/**
	 * @throws InvalidValuesException 이메일이나 비밀번호가 비었거나, 이메일의 형식이 틀리다
	 * @throws InvalidCredentialsException 이메일이 없거나 비밀번호가 틀리다. 어느 쪽인지는 알려주지 않는다
	 */
	AccessToken login(LoginCommand command);

}
