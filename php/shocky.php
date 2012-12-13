<?php

require_once 'SafePHP.php';
Requests::register_autoloader();
$safe = new SafePHP();

mb_internal_encoding("UTF-8");
mb_regex_encoding("UTF-8");

function mb_chr($value) {
	return mb_convert_encoding("&#".$value.';','UTF-8','HTML-ENTITIES');
}
function mb_ord($char) {
    $k = mb_convert_encoding($char,'UCS-2LE','UTF-8');
    $k1 = ord(substr($k,0,1));
    $k2 = ord(substr($k,1,1));
    return $k2*256+$k1;
}
function prep_url($str = '') {
	if ($str == 'http://' OR $str == '') {
		return '';
	}

	$url = parse_url($str);

	if ( ! $url OR ! isset($url['scheme'])) {
		$str = 'http://'.$str;
	}

	return $str;
}

$output = $safe->evaluate($_POST["code"]);

if(isset($output['safe_errors'])) {
	$errors = $safe->text_errors($output['safe_errors']);
	echo $errors;
} else {
	echo $output['output'];
}
