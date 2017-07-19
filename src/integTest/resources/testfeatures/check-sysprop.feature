@test
Feature: Check that sys props are properly passed to the forked JVMs for tests

  @happypath
  Scenario: Can I haz sysPrp?
    When I do nothing
    Then the value of sysprop foo is bar