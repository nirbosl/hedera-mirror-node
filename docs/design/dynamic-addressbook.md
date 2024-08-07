# HIP-869 Dynamic Address Book

## Purpose

[HIP-869](https://hips.hedera.com/hip/hip-869) implements the HAPI endpoints for Address Book management within the
current Address Book implementation.
In this phase, the mirror node processes the new transactions and service_endpoint information,
then returns that information through its existing APIs.

## Goals

- Ingest the following transactions and persist them to the database:

  - `NodeCreate`
  - `NodeDelete`
  - `NodeUpdate`

- Expose `domain_name` in the `/network/nodes` REST endpoint
- Expose `domain_name` in the grpc network service

## Architecture

### Database

```sql

alter table address_book_service_endpoint
    add column if not exists domain_name varchar(253) default null;

alter table address_book_service_endpoint drop constraint address_book_service_endpoint_pkey;

create index if not exists address_book_service_endpoint__timestamp_node_id
  on address_book_service_endpoint (consensus_timestamp , node_id);

```

### Importer

#### Node Transactions Parsing

When parsing node transactions,

- Persist `transaction_bytes` and `transaction_record_bytes` to the `transaction` table for `NodeUpdate`,`NodeCreate` and `NodeDelete`.

Update the `AddressBookServiceImpl` to persist the `domain_name` in `address_book_service_endpoint`

#### Domain

- Modify `AddressBookServiceEndpoint` domain object to add `domain_name`.

#### Transaction Handlers

Write `transaction_bytes` and `transaction_record_bytes` in the following handlers:

- Add `NodeCreateTransactionHandler`
- Add `NodeUpdateTransactionHandler`
- Add `NodeDeleteTransactionHandler`

### GRPC API

- Update the `NetworkController` to add `domain_name` to the service endpoint.

### REST API

Update the `/api/v1/network/nodes` endpoint to return `domain_name`

Response:

```json
{
  "nodes": [
    {
      "description": "address book 1",
      "file_id": "0.0.102",
      "max_stake": 50000,
      "memo": "0.0.4",
      "min_stake": 1000,
      "node_account_id": "0.0.4",
      "node_cert_hash": "0x01d173753810c0aae794ba72d5443c292e9ff962b01046220dd99f5816422696e0569c977e2f169e1e5688afc8f4aa16",
      "node_id": 1,
      "public_key": "0x4a5ad514f0957fa170a676210c9bdbddf3bc9519702cf915fa6767a40463b96f",
      "reward_rate_start": 1000000,
      "service_endpoints": [
        {
          "domain_name": "examplenode.com",
          "ip_address_v4": "128.0.0.6",
          "port": 50216
        }
      ],
      "stake": 20000,
      "stake_not_rewarded": 19900,
      "stake_rewarded": 100,
      "staking_period": {
        "from": "1655164800.000000000",
        "to": "1655251200.000000000"
      },
      "timestamp": {
        "from": "187654.000123457",
        "to": null
      }
    }
  ],
  "links": {
    "next": null
  }
}
```

## Non-Functional Requirements

- Ingest new transaction types at the same rate as consensus nodes

## Open Questions

1. Will the `admin_key` be included in `NodeInfo`, `Node` and `NodeCreateTransactionBody`? It's also important to
   determine where the mirror_node will get this value from and how it will store the data.
