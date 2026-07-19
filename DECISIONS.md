* `UUID` primary key, generated in application - no need for PostgreSQL to obtain a value from a sequence or identity column.
We will ganarate it using `UUID.randomUUID()` so it will be UUIDv4. UUIDv7 can be the production improvement because it's time-ordered and avoids B-tree fragmentation.
* `state` as `VARCHAR`. We will enforce valid values in the application layer, but the database table also contains check constraint (this constraint is being checked by integration test).
* `creation_time` as `TIMESTAMPTZ` so its value will not be ambiguous compare to `TIMESTAMP`.
* `LOWER(brand)` index for case-insensitive brand filtering.
* `version` column for optimistic locking.
* The initial state for a newly created device is `AVAILABLE`.
* Input normalization: leading/trailing whitespaces are trimmed rather than rejected; A user can put a value with such whitespaces accidentally, so a padding is not meaningful here.
* When trying to change name or brand we check the device state actually allows it, because it answers more fundamental question "May this operation happen at all" while the name/brand value validation answers "is the payload well-formed".
* PATCH uses null-as-absent semantics. Fields that are omitted or explicitly provided as null are left unchanged. All current Device fields are mandatory and cannot legitimately be cleared. Consequently, distinguishing an omitted field from an explicit null would not provide useful domain behavior. If nullable fields are introduced later, JSON Merge Patch, for example, should be considered.