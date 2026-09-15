Casing

    Variables & Methods: lowerCamelCase (totalAmount, processOrder())

    Constants (static final): UPPER_SNAKE_CASE (MAX_RETRY_LIMIT)

Methods = Verbs

    Describe actions: findUser(), calculateTax(), sendReport()

    Conversions: toDto(), asList()

    Getters/Setters: getName(), setName()

Variables = Nouns

    Describe the data: orderId, userAccount

    Use plurals for collections: List<User> users (avoid userList or usersArray)

Booleans = Assertions

    Use is, has, can, should: isActive, hasAccess, canEdit()

    Avoid negatives (use isAvailable, not isNotAvailable)

General Hygiene

    No type encoding: customer, not customerStr or accountObj

    No vague abbreviations: configuration, not cfg; calculate, not calc

    Single letters only for tiny scopes: i (loops), e (catch blocks)