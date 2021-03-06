package community.flock.eco.workday.controllers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import community.flock.eco.feature.user.forms.UserAccountPasswordForm
import community.flock.eco.feature.user.services.UserAccountService
import community.flock.eco.feature.user.services.UserSecurityService
import community.flock.eco.feature.user.services.UserService
import community.flock.eco.workday.Application
import community.flock.eco.workday.forms.PersonForm
import community.flock.eco.workday.forms.SickdayForm
import community.flock.eco.workday.model.Person
import community.flock.eco.workday.model.SickdayStatus
import community.flock.eco.workday.repository.SickdayRepository
import community.flock.eco.workday.services.PersonService
import community.flock.eco.workday.utils.dayFromLocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@ActiveProfiles(profiles = ["test"])
class SickdayControllerTest {
//    companion object {
//        // init {}
//
//        private const val baseUrl: String = "/api/sickdays"
//
//        private lateinit var user: RequestPostProcessor
//        @Autowired private lateinit var userAccountService: UserAccountService
//
//        @BeforeClass
//        @JvmStatic
//        fun setup() {
//            user = UserAccountPasswordForm(
//                    email = "admin@reynholm-industries.co.uk",
//                    name = "Administrator",
//                    authorities = setOf(),
//                    password = "admin"
//            )
//                    .run { userAccountService.createUserAccountPassword(this) }
//                    .run { UserSecurityService.UserSecurityPassword(this) }
//                    .run { user(this) }
//        }
//    }

    private val baseUrl: String = "/api/sickdays"
    private val adminUserEmail: String = "admin@reynhom-industries.co.uk"

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var mapper: ObjectMapper

    @Autowired
    private lateinit var personService: PersonService

    @Autowired
    private lateinit var userAccountService: UserAccountService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var repository: SickdayRepository

    private lateinit var user: RequestPostProcessor
    private lateinit var persons: MutableSet<Person>

    @Before
    fun setup() {
        user = UserAccountPasswordForm(
            email = adminUserEmail,
            name = "Administrator",
            authorities = setOf(
                "PersonAuthority.ADMIN",
                "PersonAuthority.READ",
                "PersonAuthority.WRITE",
                "SickdayAuthority.ADMIN",
                "SickdayAuthority.READ",
                "SickdayAuthority.WRITE"
            ),
            password = "admin")
            .run { userAccountService.createUserAccountPassword(this) }
            .run { UserSecurityService.UserSecurityPassword(this) }
            .run { user(this) }

        persons = createPersonsForTesting()
    }

    @After
    fun teardown() {
        persons = mutableSetOf()
        repository.deleteAll()
        userAccountService.findUserAccountPasswordByUserEmail(adminUserEmail)
            ?.apply { userService.delete(this.user.code) }
    }

    @Test
    fun `should get all sickdays from all users`() {
        val sickdays = createSickdaysForPersons()

        mvc.perform(get(baseUrl)
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(sickdays)))
    }

    @Test
    fun `should get all sickdays from a single user`() {
        val person = persons.elementAt(0)
        val sickdays = createSickdaysForPersons()
        val personSickdays = sickdays.filter { it.get("person").textValue() == person.code }

        mvc.perform(get("$baseUrl?code=${person.code}")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(personSickdays)))
    }

    @Test
    fun `should get active sickday from all users`() {
        val sickdays = createSickdaysForPersons()
        val activeSickdays = sickdays.filter { it.get("status").textValue() == SickdayStatus.SICK.toString() }

        mvc.perform(get("$baseUrl?status=${SickdayStatus.SICK}")
            .with(user).accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(activeSickdays)))
    }

    @Test
    fun `should get active sickday from a single user`() {
        val person = persons.elementAt(0)
        val sickdays = createSickdaysForPersons()
        val personActiveSickdays = sickdays.filter {
            it.get("status").textValue() == SickdayStatus.SICK.toString() && it.get("person").textValue() == person.code
        }

        mvc.perform(get("$baseUrl?status=${SickdayStatus.SICK}&code=${person.code}")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(personActiveSickdays)))
    }

    @Test
    fun `should get archived sickday from all users`() {
        val sickdays = createSickdaysForPersons()
        val archivedSickdays = sickdays.filter { it.get("status").textValue() == SickdayStatus.HEALTHY.toString() }

        mvc.perform(get("$baseUrl?status=${SickdayStatus.HEALTHY}")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(archivedSickdays)))
    }

    @Test
    fun `should get archived sickday from a single users`() {
        val person = persons.elementAt(0)
        val sickdays = createSickdaysForPersons()
        val personArchivedSickdays = sickdays.filter {
            it.get("status").textValue() == SickdayStatus.HEALTHY.toString() && it.get("person").textValue() == person.code
        }

        mvc.perform(get("$baseUrl?status=${SickdayStatus.HEALTHY}&code=${person.code}")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(content().json(mapper.writeValueAsString(personArchivedSickdays)))
    }

    @Test
    fun `should get a sickday via GET-Method`() {
        /* DRY-Block */
        val person = persons.elementAt(0)
        val sickdayForm = SickdayForm(
            description = "Fire! Fire!",
            status = SickdayStatus.SICK,
            from = dayFromLocalDate(),
            to = dayFromLocalDate(4),
            days = listOf(8, 8, 8, 8),
            hours = 42,
            personCode = person.code
        )
        /* DRY-Block */
        get(sickdayForm, getSickdayCodeFrom(sickdayForm, person.code))
    }

    @Test
    fun `should add a sickday via POST-Method`() {
        /* DRY-Block */
        val person = persons.elementAt(0)
        val sickdayForm = SickdayForm(
            description = "Fire! Fire!",
            status = SickdayStatus.SICK,
            from = dayFromLocalDate(),
            to = dayFromLocalDate(4),
            days = listOf(8, 8, 8, 8),
            hours = 42,
            personCode = person.code
        )
        /* DRY-Block */
        post(sickdayForm, person.code)
    }

    @Test
    fun `should change a sickday via PUT-Method`() {
        /* DRY-Block */
        val person = persons.elementAt(0)
        val sickdayForm = SickdayForm(
            description = "Fire! Fire!",
            status = SickdayStatus.SICK,
            from = dayFromLocalDate(),
            to = dayFromLocalDate(4),
            days = listOf(8, 8, 8, 8),
            hours = 42,
            personCode = person.code
        )
        /* DRY-Block */
        val sickdayCode = getSickdayCodeFrom(sickdayForm, person.code)

        // update the sickdayForm
        sickdayForm.status = SickdayStatus.HEALTHY

        put(sickdayForm, sickdayCode, person.code)
    }

    @Test
    fun `should delete a sickday via DELETE-Method`() {
        /* DRY-Block */
        val person = persons.elementAt(0)
        val sickdayForm = SickdayForm(
            description = "Fire! Fire!",
            status = SickdayStatus.SICK,
            from = dayFromLocalDate(),
            to = dayFromLocalDate(4),
            days = listOf(8, 8, 8, 8),
            hours = 42,
            personCode = person.code
        )
        /* DRY-Block */
        val sickdayCode = getSickdayCodeFrom(sickdayForm, person.code)

        mvc.perform(delete("$baseUrl/$sickdayCode")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isNoContent)

        mvc.perform(get("$baseUrl/$sickdayCode")
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `expect to retrieve a NOT_AUTHORIZED 403 return for gettings a sickday of another person without admin permissions`() {
    }

    @Test
    fun `expect to retrieve a NOT_AUTHORIZED 403 return for creating a sickday of another person without admin permissions`() {
    }

    @Test
    fun `expect to retrieve a NOT_AUTHORIZED 403 return for updating a sickday of another person without admin permissions`() {
    }

    @Test
    fun `expect to retrieve a NOT_AUTHORIZED 403 return for deleting a sickday of another person without admin permissions`() {
    }

    // *-- utility functions --*
    /**
     *
     */
    private fun post(sickdayForm: SickdayForm, personCode: String) = mvc.perform(
        post(baseUrl)
            .with(user)
            .content(mapper.writeValueAsString(sickdayForm))
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
        .andExpect(status().isOk)
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("\$.hours").value(sickdayForm.hours))
        .andExpect(jsonPath("\$.description").value(sickdayForm.description))
        .andExpect(jsonPath("\$.status").value(sickdayForm.status.toString()))
        .andExpect(jsonPath("\$.person").exists())
        .andExpect(jsonPath("\$.person").isNotEmpty)
        .andExpect(jsonPath("\$.person").value(personCode))

    /**
     *
     */
    private fun get(sickdayForm: SickdayForm, code: String? = null) {
        var get = get(baseUrl)
        if (code is String)
            get = get("$baseUrl/$code")

        mvc.perform(get
            .with(user)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_UTF8))
            .andExpect(jsonPath("\$.description").value(sickdayForm.description))
            .andExpect(jsonPath("\$.status").value(sickdayForm.status.toString()))
            .andExpect(jsonPath("\$.hours").isNumber)
            .andExpect(jsonPath("\$.hours").value(sickdayForm.hours))
            .andExpect(jsonPath("\$.person").exists())
            .andExpect(jsonPath("\$.person").isNotEmpty)
            .andExpect(jsonPath("\$.person").value(sickdayForm.personCode))
    }

    /**
     *
     */
    private fun put(sickdayForm: SickdayForm, code: String, personCode: String) = mvc.perform(
        put("$baseUrl/$code")
            .with(user)
            .content(mapper.writeValueAsString(sickdayForm))
            .contentType(APPLICATION_JSON)
            .accept(APPLICATION_JSON))
        .andExpect(status().isOk)
        .andExpect(content().contentType(APPLICATION_JSON_UTF8))
        .andExpect(jsonPath("\$.hours").value(sickdayForm.hours))
        .andExpect(jsonPath("\$.description").value(sickdayForm.description))
        .andExpect(jsonPath("\$.status").value(sickdayForm.status.toString()))
        .andExpect(jsonPath("\$.person").exists())
        .andExpect(jsonPath("\$.person").isNotEmpty)
        .andExpect(jsonPath("\$.person").value(personCode))

    /**
     *
     */
    private fun getSickdayCodeFrom(sickdayForm: SickdayForm, personCode: String) = post(sickdayForm, personCode)
        .andReturn()
        .response
        .contentAsString
        .run { mapper.readTree(this) }
        .run { this.get("code").textValue() }

    /**
     *
     */
    private fun createPersonsForTesting(): MutableSet<Person> = mutableSetOf(
        PersonForm(
            firstname = "Maurice",
            lastname = "Moss",
            email = "maurice@reynholm-industries.co.uk",
            position = "Software application developer",
            userCode = null)
            .run { personService.create(this)!! },
        PersonForm(
            firstname = "Roy",
            lastname = "Trenneman",
            email = "roy@reynholm-industries.co.uk",
            position = "Support technican",
            userCode = null)
            .run { personService.create(this)!! },
        PersonForm(
            firstname = "Jen",
            lastname = "Barber",
            email = "jen@reynholm-industries.co.uk",
            position = "Head of IT",
            userCode = null)
            .run { personService.create(this)!! },
        PersonForm(
            firstname = "Richmond",
            lastname = "Avenal",
            email = "",
            position = "",
            userCode = null)
            .run { personService.create(this)!! }
    )

    /**
     *
     */
    private fun createSickdaysForPersons(sickdays: MutableSet<JsonNode> = mutableSetOf()): MutableSet<JsonNode> {
        persons.forEach {
            val sickdayForms = mutableListOf(
                SickdayForm(
                    description = "Sick - ${it.firstname}",
                    status = SickdayStatus.SICK,
                    from = dayFromLocalDate(),
                    to = dayFromLocalDate(4),
                    days = listOf(8, 8, 8, 8),
                    hours = 42,
                    personCode = it.code
                ),
                SickdayForm(
                    description = "Healthy - ${it.firstname}",
                    status = SickdayStatus.SICK,
                    from = dayFromLocalDate(),
                    to = dayFromLocalDate(4),
                    days = listOf(8, 8, 8, 8),
                    hours = 42,
                    personCode = it.code
                )
            )

            val iter = sickdayForms.listIterator()
            while (iter.hasNext()) {
                val sickdayForm = iter.next()
                val respCode = getSickdayCodeFrom(sickdayForm, it.code)

                // a post always create a sickday with status SICK, regardless of what is passed
                // set sickdayForm status to HEALTHY for every healthy sickday and then update the sickday via PUT
                if (iter.nextIndex() % 2 == 0)
                    sickdayForm.status = SickdayStatus.HEALTHY
                sickdays.add(
                    put(sickdayForm, respCode, it.code)
                        .andReturn()
                        .response
                        .contentAsString
                        .run { mapper.readTree(this) }
                )
            }
        }
        return sickdays
    }
}
