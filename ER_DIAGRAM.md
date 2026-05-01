# Cosmetics Shop ER Diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        string email UK
        string password_hash
        string role_type
        string city
        datetime created_at
    }

    CUSTOMER_PROFILES {
        bigint id PK
        bigint user_id FK_UK
        int age
        string gender
        string city
        string membershipType
        double totalSpend
        int itemsPurchased
        double averageRating
        string satisfactionLevel
        string preferredCategory
    }

    STORES {
        bigint id PK
        string name
        string status
        bigint owner_id FK
    }

    CATEGORIES {
        bigint id PK
        string name
        bigint parent_category_id FK
        string description
    }

    PRODUCTS {
        bigint id PK
        bigint store_id FK
        bigint category_id FK
        string sku UK
        string stock_code
        string name
        text description
        decimal unit_price
        string currency_code
        decimal normalized_unit_price_usd
        text image_url
        int stock_quantity
        string status
        string style
        string product_importance
        datetime created_at
        datetime updated_at
    }

    ORDERS {
        bigint id PK
        bigint user_id FK
        bigint store_id FK
        double grand_total
        string source_order_id
        string invoice_no
        string increment_id
        string payment_method
        string status
        string fulfilment
        string sales_channel
        string ship_service_level
        string currency_code
        double normalized_grand_total_usd
        datetime order_date
        datetime created_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        int quantity
        double unit_price
        double subtotal
    }

    SHIPMENTS {
        bigint id PK
        bigint order_id FK
        string warehouseBlock
        string modeOfShipment
        string trackingNumber
        string status
        int customerCareCalls
        int customerRating
        double shippingCost
        int priorPurchases
        string productImportance
        double discountOffered
        datetime estimatedDeliveryAt
        datetime deliveredAt
    }

    REVIEWS {
        bigint id PK
        bigint product_id FK
        bigint user_id FK
        int star_rating
        string review_text
        int helpfulVotes
        int totalVotes
        datetime created_at
    }

    USERS ||--o| CUSTOMER_PROFILES : has
    USERS ||--o{ ORDERS : places
    USERS ||--o{ REVIEWS : writes
    USERS ||--o{ STORES : owns

    STORES ||--o{ PRODUCTS : sells
    STORES ||--o{ ORDERS : receives

    CATEGORIES ||--o{ PRODUCTS : classifies
    CATEGORIES ||--o{ CATEGORIES : parent_of

    ORDERS ||--|{ ORDER_ITEMS : contains
    PRODUCTS ||--o{ ORDER_ITEMS : included_in

    ORDERS ||--o| SHIPMENTS : ships_as

    PRODUCTS ||--o{ REVIEWS : receives
}
```

## Notes

- `CUSTOMER_PROFILES.user_id` is unique, so each user can have at most one customer profile.
- `SHIPMENTS.order_id` has a unique index, so each order can have at most one shipment.
- `STORES.owner_id` and `CATEGORIES.parent_category_id` are stored as ID columns rather than explicit JPA object relationships, but they are included as conceptual foreign keys in the diagram.
- Fields marked `@Transient` in the Java models are excluded because they are not stored in the database.
