package community.flock.eco.workday.controllers

import community.flock.eco.core.utils.toNullable
import community.flock.eco.core.utils.toResponse
import community.flock.eco.feature.user.model.User
import community.flock.eco.feature.user.repositories.UserRepository
import community.flock.eco.workday.forms.HolidayForm
import community.flock.eco.workday.model.Holiday
import community.flock.eco.workday.repository.PeriodRepository
import community.flock.eco.workday.services.HolidayService
import community.flock.eco.workday.services.isAdmin
import java.security.Principal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/holidays")
class HolidayController(
    private val userRepository: UserRepository,
    private val periodRepository: PeriodRepository,
    private val service: HolidayService
) {

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('HolidayAuthority.READ')")
    fun findByCode(@PathVariable code: String, principal: Principal): ResponseEntity<Holiday> = principal.findUser()
            ?.let { user ->
                service.findByCode(code)
            }.toResponse()

    @GetMapping
    @PreAuthorize("hasAuthority('HolidayAuthority.READ')")
    fun findAll(@RequestParam(required = false) userCode: String?, principal: Principal): ResponseEntity<Iterable<Holiday>> =
            principal.findUser()
                    ?.let { user ->
                        if (user.isAdmin() && userCode != null) {
                            service.findAllByUserCode(userCode)
                        } else {
                            service.findAllByUserCode(user.code)
                        }
                    }.toResponse()

    @PostMapping
    @PreAuthorize("hasAuthority('HolidayAuthority.WRITE')")
    fun post(@RequestBody form: HolidayForm, principal: Principal): ResponseEntity<Holiday> = principal
            .findUser()
            ?.let {
                if (!it.isAuthorizedForUserCode(form.userCode)) {
                    form.copy(userCode = it.code)
                } else {
                    form.copy(userCode = form.userCode ?: it.code)
                }
            }
            ?.let {
                service.create(it)
            }
            .toResponse()

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('HolidayAuthority.WRITE')")
    fun put(@PathVariable code: String, @RequestBody form: HolidayForm, principal: Principal): ResponseEntity<Any> =
            principal
                    .findUser()
                    ?.let {
                        if (it.isAuthorizedForHoliday(code)) {
                            service.update(code, form)
                        } else {
                            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        }
                    }
                    .toResponse()

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('HolidayAuthority.WRITE')")
    fun delete(@PathVariable code: String, principal: Principal): ResponseEntity<Any> {
        return principal
                .findUser()
                ?.let {
                    if (it.isAuthorizedForHoliday(code)) {
                        service.delete(code)
                    } else {
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    }
                }
                .toResponse()
    }

    private fun Principal.findUser(): User? = userRepository
            .findByCode(this.name)
            .toNullable()

    fun User.isAuthorizedForUserCode(userCode: String?): Boolean = this.isAdmin() || this.code.equals(userCode)

    fun User.isAuthorizedForHoliday(code: String): Boolean = this.isAdmin() || this.equals(service.findByCode(code)?.user)
}
