Feature: Authorization server HTTP contracts
  The authorization server authenticates clients and publishes public signing keys.

  Scenario: Authenticated client obtains a JWT
    When an authenticated client requests a JWT from the authorization endpoint
    Then the response status is 200
    And the token response contains a signed bearer JWT

  Scenario: Anonymous token issuance is denied
    When an anonymous client requests a token
    Then the response status is 401

  Scenario: Public signing keys can be retrieved from JWKS
    When a client requests the authorization server JWKS endpoint
    Then the response status is 200
    And the JWKS response contains a public RSA signing key
