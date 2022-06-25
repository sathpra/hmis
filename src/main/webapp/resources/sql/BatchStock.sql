-- update stock set `STOCK`=0.0 where `DEPARTMENT_ID` = 2;
select id,`STOCK`, `ITEMBATCH_ID`, `CODE`,`DEPARTMENT_ID` from stock where `DEPARTMENT_ID` = 2 order by stock desc;
