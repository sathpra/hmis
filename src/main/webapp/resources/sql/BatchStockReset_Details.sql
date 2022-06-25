set foreign_key_checks=0;
select sum(stock) from stock where `DEPARTMENT_ID` <> 1334018;
select * from stock where `DEPARTMENT_ID` = 1334018;
select `ID`,`NAME`,`RETIRED` from department where id=1334018;
-- Delete from stock
-- where `DEPARTMENT_ID` = 1334018;
-- set foreign_key_checks=1;