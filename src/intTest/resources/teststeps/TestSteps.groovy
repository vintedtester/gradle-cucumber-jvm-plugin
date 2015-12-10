package cucumber.steps

import cucumber.api.java.en.When
import cucumber.api.java.en.Then

class TestSteps {
    @When('I throw an exception')
    def iThrowAnException() {
        throw new RuntimeException()
    }

    @When('I do nothing')
    def iDoNothing() {

    }

    @Then('I pass my assertion')
    def iPassMyAssertion() {
        assert 1 == 1
    }

    @Then('I fail my assertion')
    def iFailMyAssertion() {
        assert 1 == 2
    }
}