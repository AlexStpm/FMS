package org.alexstpm.fms;

import org.alexstpm.fms.account.model.Account;
import org.alexstpm.fms.account.model.AccountType;
import org.alexstpm.fms.account.repository.AccountRepository;
import org.alexstpm.fms.audit.model.Audit;
import org.alexstpm.fms.audit.repository.AuditRepository;
import org.alexstpm.fms.auth.dto.LoginRequest;
import org.alexstpm.fms.auth.dto.RefreshRequest;
import org.alexstpm.fms.auth.dto.TokenResponse;
import org.alexstpm.fms.department.model.Department;
import org.alexstpm.fms.department.model.DepartmentMembership;
import org.alexstpm.fms.department.repository.DepartmentMembershipRepository;
import org.alexstpm.fms.department.repository.DepartmentRepository;
import org.alexstpm.fms.security.oauth2.GoogleOAuth2SuccessHandler;
import org.alexstpm.fms.security.repository.RefreshTokenRepository;
import org.alexstpm.fms.transaction.dto.CreateTransactionRequest;
import org.alexstpm.fms.transaction.model.TransactionType;
import org.alexstpm.fms.transaction.repository.TransactionRepository;
import org.alexstpm.fms.user.dto.CreateUserRequest;
import org.alexstpm.fms.user.model.Role;
import org.alexstpm.fms.user.model.User;
import org.alexstpm.fms.user.repository.UserRepository;
import org.alexstpm.fmsstartuplogger.StartupInfoLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class FmsApplicationTests {

    private static final String PASSWORD = "correct-password-123";

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private DepartmentMembershipRepository membershipRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    @MockitoSpyBean
    private AuditRepository auditRepository;

    @Test
    void contextLoadsAndLoadsStarterAutomatically() {
        // given / when
        StartupInfoLogger startupInfoLogger = applicationContext.getBean(StartupInfoLogger.class);

        // then
        assertNotNull(startupInfoLogger);
    }

    @Test
    void passwordLoginReturnsBothTokensAndAccessTokenAuthenticatesRequest() throws Exception {
        // given
        String email = uniqueEmail("jwt");
        createUser(email, Role.ACCOUNTANT);
        LoginRequest request = new LoginRequest(email, PASSWORD);

        // when
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        TokenResponse tokens = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), TokenResponse.class);

        // then
        assertTrue(tokens.expiresIn() > 0);
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void passwordLoginRejectsWrongPassword() throws Exception {
        // given
        String email = uniqueEmail("bad-login");
        createUser(email, Role.ACCOUNTANT);

        // when / then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void refreshRotatesTokensAndRejectsReuseOfOldRefreshToken() throws Exception {
        // given
        String email = uniqueEmail("refresh");
        createUser(email, Role.ACCOUNTANT);
        TokenResponse initialTokens = login(email);

        // when
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshRequest(initialTokens.refreshToken()))))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        TokenResponse rotatedTokens = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), TokenResponse.class);
        assertNotEquals(initialTokens.refreshToken(), rotatedTokens.refreshToken());

        // when / then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshRequest(initialTokens.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifiedGoogleUserReceivesTokensAndOAuthSessionIsCleared() throws Exception {
        // given
        String email = uniqueEmail("google");
        createUser(email, Role.ACCOUNTANT);
        long refreshTokensBefore = refreshTokenRepository.count();
        OAuth2AuthenticationToken authentication = googleAuthentication(email, true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        googleOAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertEquals(200, response.getStatus());
        TokenResponse tokens = objectMapper.readValue(response.getContentAsString(), TokenResponse.class);
        assertFalse(tokens.accessToken().isBlank());
        assertFalse(tokens.refreshToken().isBlank());
        assertNull(request.getSession(false));
        assertEquals(refreshTokensBefore + 1, refreshTokenRepository.count());
    }

    @Test
    void googleLoginRejectsUnverifiedEmail() throws Exception {
        // given
        OAuth2AuthenticationToken authentication = googleAuthentication(
                uniqueEmail("unverified"), false);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        googleOAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertEquals(401, response.getStatus());
    }

    @Test
    void googleLoginRejectsEmailWithoutExistingUser() throws Exception {
        // given
        OAuth2AuthenticationToken authentication = googleAuthentication(
                uniqueEmail("unknown-google"), true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        googleOAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertEquals(401, response.getStatus());
    }

    @Test
    void onlyAdminCanCreateUsersAndAuditorCanReadUsers() throws Exception {
        // given
        CreateUserRequest createRequest = new CreateUserRequest(
                uniqueEmail("created"), PASSWORD, Role.ACCOUNTANT);
        String body = objectMapper.writeValueAsString(createRequest);

        // when / then
        mockMvc.perform(post("/api/v1/users")
                        .with(user("accountant").roles("ACCOUNTANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/users")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/users")
                        .with(user("auditor").roles("AUDITOR")))
                .andExpect(status().isOk());
    }

    @Test
    void failedAuditWriteRollsBackTransferCreation() throws Exception {
        // given
        String email = uniqueEmail("atomic-transfer");
        User accountant = createUser(email, Role.ACCOUNTANT);
        Department department = new Department();
        department.setName("Test department");
        department.setCode("D-" + UUID.randomUUID());
        department.setSupervisor(accountant);
        department = departmentRepository.saveAndFlush(department);

        Account source = account(department);
        Account destination = account(department);
        source = accountRepository.saveAndFlush(source);
        destination = accountRepository.saveAndFlush(destination);

        DepartmentMembership membership = new DepartmentMembership();
        membership.setUser(accountant);
        membership.setDepartment(department);
        membershipRepository.saveAndFlush(membership);

        TokenResponse tokens = login(email);
        long transactionCountBefore = transactionRepository.count();
        CreateTransactionRequest transfer = new CreateTransactionRequest(
                TransactionType.TRANSFER,
                new BigDecimal("25.00"),
                "USD",
                LocalDate.of(2026, 10, 10),
                source.getId(),
                destination.getId(),
                null,
                null);
        doThrow(new IllegalStateException("simulated audit failure"))
                .when(auditRepository).save(any(Audit.class));

        // when / then
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isInternalServerError());

        // then
        assertEquals(transactionCountBefore, transactionRepository.count());
    }

    private User createUser(String email, Role role) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .enabled(true)
                .build();
        return userRepository.saveAndFlush(user);
    }

    private TokenResponse login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }

    private Account account(Department department) {
        Account account = new Account();
        account.setType(AccountType.SETTLEMENT_ACCOUNT);
        account.setCurrency("USD");
        account.setDepartment(department);
        return account;
    }

    private static OAuth2AuthenticationToken googleAuthentication(String email, boolean verified) {
        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "test-id-token",
                issuedAt,
                issuedAt.plusSeconds(300),
                Map.of("sub", UUID.randomUUID().toString(), "email", email, "email_verified", verified));
        OidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
