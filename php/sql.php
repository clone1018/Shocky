<?php

$db["host"] = "localhost";
$db["user"] = "root";
$db["pass"] = "loBM7YMueS";
$db["db"] = "shocky";
$db["prefix"] = "";
_sqlConnect();
unset($db["host"],$db["user"],$db["pass"],$db["db"]);
define("_MAGIC_QUOTES", (ini_get('magic_quotes_gpc') ? TRUE : FALSE));

if (!isset($_GET["type"]) die();
if (!isset($_GET["q"]) die();
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
		echo sql2json($_GET["q"]);
	} break;
}

function _sqlConnect() {
	global $db;

	$db_connect = mysql_connect($db["host"],$db["user"],$db["pass"]);
	if (!$db_connect) die("SQL Error: ".mysql_errno().": ".mysql_error(),true);
	$db_select = mysql_select_db($db["db"]);
	if (!$db_select) die("SQL Error: ".mysql_errno().": ".mysql_error(),true);
}
function sqlQuery($query) {
	$r = mysql_query($query);
	if (!$r) die("SQL Error: ".mysql_errno().": ".mysql_error()."<br />Query: ".$query,true);
	return $r;
}
function sqlCount($table) {
	$r = mysql_query("SELECT COUNT(*) FROM ".$table);
	if (!$r) die("SQL Error: ".mysql_errno().": ".mysql_error(),true);
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

function sql2json($query) {
	$data_sql = mysql_query($query) or die("'';//".mysql_error());
	$json_str = "";
	if ($total = mysql_num_rows($data_sql)) {
		$json_str .= "[\n";
		$row_count = 0;	
		while ($data = mysql_fetch_assoc($data_sql)) {
			if(count($data) > 1) $json_str .= "{";
			$count = 0;
			foreach ($data as $key => $value) {
				$json_str .= count($data) > 1 ? "\"$key\":\"$value\"" : "\"$value\"";
				$count++;
				if($count < count($data)) $json_str .= ",";
			}
			$row_count++;
			if(count($data) > 1) $json_str .= "}";
			if($row_count < $total) $json_str .= ",";
		}
		$json_str .= "]";
	}
	return $json_str;
}

?>