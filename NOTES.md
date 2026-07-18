I used the next query to generate and insert test data (200K rows) into the table device:
```sql
insert into device (
	id,
	name,
	brand,
	state,
	creation_time,
	version
)
select 
	gen_random_uuid(),
	'Device-' || g,
	case 
		when g = 1 then 'RareBrandZX'
		WHEN g % 2000 = 0 THEN 'APPLE'
        WHEN g % 1000 = 0 THEN 'Apple'
        WHEN g % 3 = 0 THEN 'Samsung'
        WHEN g % 3 = 1 THEN 'Nokia'
        ELSE 'Xiaomi'
	end,
	CASE
        WHEN g % 3 = 0 THEN 'AVAILABLE'
        WHEN g % 3 = 1 THEN 'IN_USE'
        ELSE 'INACTIVE'
    END,
    CURRENT_TIMESTAMP - ((g % 365) * INTERVAL '1 day'),
    0
from generate_series(1, 200000) as g;
```
Then I observed how planner chooses SELECT execution with and without using index.
Query using index: 
```sql
explain ANALYZE
select * from device
WHERE LOWER(brand) = 'apple';
```
Result using index:
```
"QUERY PLAN"
Bitmap Heap Scan on device  (cost=12.04..1829.49 rows=1000 width=59) (actual time=0.042..0.203 rows=200 loops=1)
  Recheck Cond: (lower((brand)::text) = 'apple'::text)
  Heap Blocks: exact=200
  ->  Bitmap Index Scan on idx_device_brand_lower  (cost=0.00..11.79 rows=1000 width=0) (actual time=0.023..0.024 rows=200 loops=1)
        Index Cond: (lower((brand)::text) = 'apple'::text)
Planning Time: 0.073 ms
Execution Time: 0.238 ms
```
Query without using index:
```sql
explain ANALYZE
select * from device
WHERE UPPER(brand) = 'APPLE';
```
Result without using index:
```
"QUERY PLAN"
Gather  (cost=1000.00..5137.71 rows=1000 width=59) (actual time=0.271..18.057 rows=200 loops=1)
  Workers Planned: 1
  Workers Launched: 1
  ->  Parallel Seq Scan on device  (cost=0.00..4037.71 rows=588 width=59) (actual time=0.113..13.977 rows=100 loops=2)
        Filter: (upper((brand)::text) = 'APPLE'::text)
        Rows Removed by Filter: 99900
Planning Time: 0.044 ms
Execution Time: 18.084 ms
```

