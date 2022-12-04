package ch.rasc.hashupgrade;

import static ch.rasc.hashupgrade.db.tables.AppUser.APP_USER;

import org.jooq.DSLContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ch.rasc.hashupgrade.db.tables.records.AppUserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

  private final PasswordEncoder passwordEncoder;

  private final DSLContext dsl;

  private final String userNotFoundEncodedPassword;

  private final SecurityContextRepository securityContextRepository;

  public AuthController(PasswordEncoder passwordEncoder, DSLContext dsl,
      SecurityContextRepository securityContextRepository) {
    this.passwordEncoder = passwordEncoder;
    this.dsl = dsl;
    this.securityContextRepository = securityContextRepository;
    this.userNotFoundEncodedPassword = this.passwordEncoder
        .encode("userNotFoundPassword");
  }

  @PostMapping("/signin")
  public String signin(@RequestParam String username, @RequestParam String password,
      HttpServletRequest request, HttpServletResponse response) {

    AppUserRecord appUserRecord = this.dsl.selectFrom(APP_USER)
        .where(APP_USER.USER_NAME.eq(username)).fetchOne();

    if (appUserRecord != null) {
      boolean pwMatches = this.passwordEncoder.matches(password,
          appUserRecord.getPasswordHash());
      if (pwMatches) {

        // upgrade password
        if (this.passwordEncoder.upgradeEncoding(appUserRecord.getPasswordHash())) {
          this.dsl.update(APP_USER)
              .set(APP_USER.PASSWORD_HASH, this.passwordEncoder.encode(password))
              .where(APP_USER.ID.eq(appUserRecord.getId())).execute();
        }

        AppUserDetail detail = new AppUserDetail(appUserRecord);
        AppUserAuthentication userAuthentication = new AppUserAuthentication(detail);
        SecurityContextHolder.getContext().setAuthentication(userAuthentication);
        this.securityContextRepository.saveContext(SecurityContextHolder.getContext(),
            request, response);
        return "redirect:/index.html";
      }
    }
    else {
      this.passwordEncoder.matches(password, this.userNotFoundEncodedPassword);
    }

    return "redirect:/signin.html";
  }

}
