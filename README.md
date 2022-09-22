# Liquibase_QueryDsl_Demo

#LIQUIBASE:
Providing liquibase with meta data for tables and columns dynamically to create them into a static form.
API Request Body:
[
    {
        "name": "book",
        "description": "Books table",
        "columnSet": [
            {
                "name": "id",
                "description": "ID",
                "columnType": "bigint",
                "isPk": true,
                "length": null,
                "scale": null,
                "isNullable": false,
                "isUnique": true,
                "defaultValue": null
            },
            {
                "name": "title",
                "description": "Book title",
                "columnType": "varchar",
                "isPk": false,
                "length": 255,
                "scale": null,
                "isNullable": false,
                "isUnique": false,
                "defaultValue": null
            }
        ]
    }
]

#QUERYDSL:
Using querydsl we can perform the CRUD operations on the tables without the need of having static entities or repositories

Insert API Request:
{
    "title": "FEABCD",
    "isbn": "978-1-56619-909-10"
}

Update API Request:
{
    "id": 2,
    "title": "FEABCD",
    "isbn": "978-1-56619-909-2"
}

Search API Request:
{
    "fields": [
        "id",
        "title",
        "isbn"
    ],
    "predicateSet": [
        {
            "goe": {
                "field": "id",
                "value": 0
            }
        },
        {
            "loe": {
                "field": "id",
                "value": 5
            }
        }
    ]
}

Liquibase Documentation: https://docs.liquibase.com/home.html
QueryDsl Documentation: http://querydsl.com/static/querydsl/5.0.0/apidocs/

