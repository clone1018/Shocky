<?php

if (!isset($_GET["eval"])) die();
eval($_GET["eval"]);

/*
$db["host"] = "localhost";
$db["user"] = "user";
$db["pass"] = "pass";
$db["db"] = "shocky";
*/

_sqlConnect();
unset($db);

if (!isset($_GET["type"])) die();
if (!isset($_GET["q"])) die();
switch ($_GET["type"]) {
	case "raw": {
		sqlQuery($_GET["q"]);
		die("OK");
	} break;
	case "insertid": {
		sqlQuery($_GET["q"]);
		die(mysql_insert_id());
	} break;
	case "json": {
		echo sql2json(sqlQuery($_GET["q"]));
	} break;
}

function _sqlConnect() {
	global $db;

	$db_connect = mysql_connect($db["host"],$db["user"],$db["pass"]);
	if (!$db_connect) die("SQL Error: ".mysql_errno().": ".mysql_error());
	$db_select = mysql_select_db($db["db"]);
	if (!$db_select) die("SQL Error: ".mysql_errno().": ".mysql_error());
}
function sqlQuery($query) {
	$r = mysql_query($query);
	if (!$r) die("SQL Error: ".mysql_errno().": ".mysql_error()."<br />Query: ".$query);
	return $r;
}
function sqlCount($table) {
	$r = mysql_query("SELECT COUNT(*) FROM ".$table);
	if (!$r) die("SQL Error: ".mysql_errno().": ".mysql_error());
	return sqlResult($r,0);
}
function sqlResult($result,$row) {
	return mysql_result($result,$row);
}
function sqlRows($result) {
	return mysql_num_rows($result);
}
function sqlArray($result) {
	return mysql_fetch_assoc($result);
}

function sql2json($data_sql) {
	$json_str = "";
	if ($total = sqlRows($data_sql)) {
		if ($total > 1) $json_str .= "[";
		$row_count = 0;	
		while ($data = sqlArray($data_sql)) {
			if (count($data) > 1) $json_str .= "{";
			$count = 0;
			foreach ($data as $key => $value) {
				if (count($data) > 1) $json_str .= "\"".$key."\":";
				if (is_string($value)) {
					$json_str .= "\"";
					$value = preg_replace('/\\"\\\\/',"\\$0",$value);
				}
				$json_str .= $value;
				if (is_string($value)) $json_str .= "\"";
				$count++;
				if ($count < count($data)) $json_str .= ",";
			}
			$row_count++;
			if (count($data) > 1) $json_str .= "}";
			if ($row_count < $total) $json_str .= ",";
		}
		if ($total > 1) $json_str .= "]";
	} else $json_str = "{}";
	return $json_str;
}

?>