<?php

require_once 'Requests/library/Requests.php';
Requests::register_autoloader();

/**
 * SafePHP
 * Safely runs PHP code in a chrooted env.
 * 
 * Example:
 * <code>
 * <?php
 * $safephp = new SafePHP();
 * $safephp->evaluate($code);
 * ?>
 * </code>
 * 
 * @version 1.0
 * @access public
 * @author clone1018
 */
class SafePHP {

    /**
     * SafePHP Constructor
     * Sets error handler functions and fatal shutdown function.
     * 
     * @access public
     */
    public function __construct() {
        set_error_handler(array('SafePHP', 'error_handler'));
        register_shutdown_function(array('SafePHP', 'fatal_error_handler'));
    }

    /**
     * SafePHP get_setting
     * Allows SafePHP to get it's settings in a safe and secure way.
     * 
     * @access private
     * @param string $setting
     */
    private final static function get_setting($setting) {
        switch ($setting) {
            case 'allowed_calls':
                return array('function', 'ceil', 'floor', 'fmod', 'log', 'mt_rand', 'mt_srand', 'pow', 'rand', 'sqrt', 'srand', 'empty', 'floatval', 'intval', 'is_array', 'is_binary', 'is_bool', 'is_double', 'is_float', 'is_int', 'is_integer', 'is_long', 'is_null', 'is_numeric', 'is_real', 'is_scalar', 'is_string', 'is_unicode', 'isset', 'strval', 'unset', 'array_change_key_case', 'array_chunk', 'array_combine', 'array_count_values', 'array_diff_assoc', 'array_diff_key', 'array_diff', 'array_fill_keys', 'array_fill', 'array_flip', 'array_intersect_assoc', 'array_intersect_key', 'array_intersect', 'array_key_exists', 'array_keys', 'array_merge_recursive', 'array_merge', 'array_multisort', 'array_pad', 'array_pop', 'array_product', 'array_push', 'array_rand', 'array_reverse', 'array_search', 'array_shift', 'array_slice', 'array_splice', 'array_sum', 'array_unique', 'array_unshift', 'array_values', 'array', 'arsort', 'asort', 'compact', 'count', 'current', 'each', 'end', 'in_array', 'key', 'krsort', 'ksort', 'natcasesort', 'natsort', 'next', 'pos', 'prev', 'range', 'reset', 'rsort', 'shuffle', 'sizeof', 'sort', 'chop', 'count_chars', 'explode', 'implode', 'join', 'levenshtein', 'ltrim', 'metaphone', 'money_format', 'number_format', 'rtrim', 'similar_text', 'soundex', 'str_getcsv', 'str_ireplace', 'str_pad', 'str_repeat', 'str_replace', 'str_rot13', 'str_shuffle', 'str_split', 'str_word_count', 'strcasecmp', 'strchr', 'strcmp', 'strcspn', 'stripos', 'stristr', 'strlen', 'strnatcasecmp', 'strnatcmp', 'strncasecmp', 'strncmp', 'strpbrk', 'strpos', 'strrchr', 'strrev', 'strripos', 'strrpos', 'strspn', 'strstr', 'strtolower', 'strtoupper', 'strtr', 'substr_compare', 'substr_count', 'substr_replace', 'substr', 'trim', 'ucfirst', 'ucwords', 'wordwrap');
                break;
            case 'allowed_tokens':
                return array('T_INCLUDE', 'T_AND_EQUAL', 'T_ARRAY', 'T_ARRAY_CAST', 'T_AS', 'T_BOOLEAN_AND', 'T_BOOLEAN_OR', 'T_BOOL_CAST', 'T_BREAK', 'T_CASE', 'T_CHARACTER', 'T_COMMENT', 'T_CONCAT_EQUAL', 'T_CONSTANT_ENCAPSED_STRING', 'T_CONTINUE', 'T_CURLY_OPEN', 'T_DEC', 'T_DECLARE', 'T_DEFAULT', 'T_DIV_EQUAL', 'T_DNUMBER', 'T_DO', 'T_DOUBLE_ARROW', 'T_DOUBLE_CAST', 'T_ECHO', 'T_ELSE', 'T_ELSEIF', 'T_EMPTY', 'T_ENCAPSED_AND_WHITESPACE', 'T_ENDDECLARE', 'T_ENDFOR', 'T_ENDFOREACH', 'T_ENDIF', 'T_ENDSWITCH', 'T_FOR', 'T_FOREACH', 'T_IF', 'T_INC', 'T_INT_CAST', 'T_ISSET', 'T_IS_EQUAL', 'T_IS_GREATER_OR_EQUAL', 'T_IS_IDENTICAL', 'T_IS_NOT_EQUAL', 'T_IS_NOT_IDENTICAL', 'T_IS_SMALLER_OR_EQUAL', 'T_LNUMBER', 'T_LOGICAL_AND', 'T_LOGICAL_OR', 'T_LOGICAL_XOR', 'T_MINUS_EQUAL', 'T_MOD_EQUAL', 'T_MUL_EQUAL', 'T_NUM_STRING', 'T_OR_EQUAL', 'T_PLUS_EQUAL', 'T_RETURN', 'T_SL', 'T_SL_EQUAL', 'T_SR', 'T_SR_EQUAL', 'T_STRING', 'T_STRING_CAST', 'T_STRING_VARNAME', 'T_SWITCH', 'T_UNSET', 'T_UNSET_CAST', 'T_VARIABLE', 'T_WHILE', 'T_WHITESPACE', 'T_XOR_EQUAL', 'T_PRINT', 'T_PRINT_R', 'T_FUNCTION', 'T_OBJECT_OPERATOR', 'T_DOUBLE_COLON', 'T_CLASS', 'T_CONST', 'T_EXTENDS', 'T_NEW', 'T_PRIVATE', 'T_PUBLIC', 'T_PROTECTED', 'T_CLASS_C', 'T_METHOD_C', 'T_LIST', 'T_STATIC', 'T_NAMESPACE', 'T_NS_SEPARATOR', 'T_NS_C', 'T_CLONE', 'T_TRY', 'T_CATCH', 'T_EXIT', 'T_DOC_COMMENT', 'T_GLOBAL');
                break;
            case 'disallowed_expressions':
                return array('/`/', '/(\]|\})\s*\(/', '/\$\w\w*\s*\(/',);
                break;
            case 'disabled_functions':
                return array("error_log","get_included_files", "phpinfo", "eval", "call_user_func_array", "call_user_func", "create_function", "forward_static_call_array", "forward_static_call", "func_get_arg", "func_get_args", "func_num_args", "function_exists", "get_defined_functions", "register_shutdown_function", "register_tick_function", "unregister_tick_function", "file_put_contents", "file", "ini_set", "ini_get", "mail", "setenv", "getenv", "socket_create", "socket_bind", "socket_listen", "socket_create_listen", "socket_create_pair", "socket_accept", "pcntl_fork", "exec", "passthru", "shell_exec", "system", "proc_open", "popen", "parse_ini_file", "show_source", "glob", "opendir", "readdir", "set_time_limit", "unlink", "rmdir", "mkdir", "rename", "copy", "dir", "scandir", "ftp_connect", "ftp_ssl_connect", "openlog", "syslog", "fsockopen", "define_syslog_variables", "pfsockopen", "snmp2_get", "snmp3_get", "snmp2_walk", "snmp2_real_walk", "snmp2_getnext", "snmp3_walk", "snmp3_real_walk", "snmpget", "snmpwalk", "snmpgetnext", "snmprealwalk", "snmp3_getnext", "snmpwalkoid", "ssh2_connect", "ssh2_fetch_stream", "ssh2_tunnel", "yaz_connect", "yaz_wait", "disk_free_space", "disk_total_space", "flock", "link", "tempnam", "tmpfile", "touch", "symlink", "pcntl_exec", "posix_kill", "posix_mkfifo", "posix_mknod", "fopen", "stream_socket_server", "stream_socket_client", "stream_socket_pair", "gc_disable", "ob_end_flush", "flush", "ini_get_all", "get_loaded_extensions", "ini_alter", "chmod", "chgrp", "chown", "posix_access", "posix_ctermid", "posix_errno", "posix_get_last_error", "posix_getcwd", "posix_getegid", "posix_geteuid", "posix_getgid", "posix_getgrgid", "posix_getgrnam", "posix_getgroups", "posix_getlogin", "posix_getpgid", "posix_getpgrp", "posix_getpid", "posix_getppid", "posix_getpwnam", "posix_getpwuid", "posix_getrlimit", "posix_getsid", "posix_getuid", "posix_initgroups", "posix_isatty", "posix_kill", "posix_mkfifo", "posix_mknod", "posix_setegid", "posix_seteuid", "posix_setgid", "posix_setpgid", "posix_setsid", "posix_setuid", "posix_strerror", "posix_times", "posix_ttyname", "posix_uname", "chdir", "opendir", "readdir", "debug_backtrace", "debug_print_backtrace");
                break;
        }
    }

    /**
     * SafePHP parse
     * Parses a PHP script for errors before evaling.
     * 
     * @param string $code
     * @return boolean
     */
    public function parse($code) {
        ob_start();
        $code = eval('if(0){' . $code . '}');
        ob_end_clean();
        return $code !== false;
    }

    /**
     * SafePHP evaluate
     * Evaluates PHP code in a secure manner.
     * A portion of this code has been borrowed from http://sourceforge.net/projects/evileval/
     * 
     * @param string $code
     * @return array
     */
    public function evaluate($code) {
		global $_STATE;
        $this->code = $code;
        $this->tokens = token_get_all('<?php ' . $this->code . ' ?>');
        $this->errors = array();
        $this->braces = 0;

        // Check to see if braces are balanced.
        foreach ($this->tokens as $token) {
            if ($token == '{')
                $this->braces = $this->braces + 1;
            else if ($token == '}')
                $this->braces = $this->braces - 1;
            if ($this->braces < 0) { // Closing brace before one is open
                $this->errors[0]['name'] = 'Syntax error.';
                break;
            }
        }

        if (empty($this->errors)) {
            if ($this->braces)
                $this->errors[0]['name'] = 'Unbalanced braces.';
        } elseif (!$this->parse($this->code)) {
            $this->errors[0]['name'] = 'Syntax error.';
        }

    

        // If there are no errors yet, check the code for illegal objects.
        if (empty($this->errors)) {
            unset($this->tokens[0]);
            array_pop($this->tokens);

            $i = 0;
            foreach ($this->tokens as $key => $token) {
                $i++;
                if (is_array($token)) {
                    $id = token_name($token[0]);
                    switch ($id) {
                        case('T_STRING'):
                            if (in_array($token[1], $this->get_setting('disabled_functions')) === true) {
                                $this->errors[$i]['name'] = 'Illegal function: ' . $token[1];
                                $this->errors[$i]['line'] = $token[2];
                            }
                            if ($token[1] == 'file_get_contents') {
                                $this->tokens[$i][1] = '$this->safe_file_get_contents';
                            }
                            // exit() doesn't function properly, this is a temporary bug fix.
                            if ($token[1] == 'exit') {
                                $this->tokens[$i][1] = '';
                            }
                            break;
                        default:
                            if (in_array($id, $this->get_setting('allowed_tokens')) === false) {
                                $this->errors[$i]['name'] = 'Illegal token: ' . $token[1];
                                $this->errors[$i]['line'] = $token[2];
                            }
                            break;
                    }
                }
            }
        }
        $this->code = '';
        foreach ($this->tokens as $key => $token) {
            if (is_array($token)) {
                $this->code .= $token[1];
            } else {
                $this->code .= $token;
            }
        }
		
		unset($code, $key, $token, $i, $id, $this->tokens, $this->braces);
        ob_start(array($this, 'dump_json'));
        $this->start = microtime(true);
		if (empty($this->errors))
			eval($this->code);
		ob_end_flush();
    }
	
	public function dump_json($buffer) {
		global $_STATE;
		$output = array('output' => $buffer);
        $output['debug'] = array(
            'time' => microtime(true) - $this->start,
            'version' => phpversion(),
            'memory' => memory_get_usage()
        );
        if (error_get_last())
            $output['error'] = error_get_last();
        if(!empty($this->errors))
            $output['safe_errors'] = $this->text_errors($this->errors);
		if (isset($_STATE) && !is_resource($_STATE))
			$output['data'] = $_STATE;

        return json_encode($output);
	}
    
    /**
     * SafePHP html_errors
     * Outputs errors in a nice HTML format.
     * 
     * @param array $errors
     */
    public static function html_errors($errors = null) {
        if($errors) {
            $this->errors = $errors;
            $this->errorsHTML = '<dl>';
            foreach ($this->errors as $error) {
                if (isset($error['line']) && $error['line']) {
                    $this->errorsHTML .= '<dt>Line ' . $error['line'] . '</dt>';
                    $error['line']++;
                }
                $this->errorsHTML .= '<dd>' . $error['name'] . '</dd>';
            }
            $this->errorsHTML .= '</dl>';
            return($this->errorsHTML);
        }
    }
    
    /**
     * SafePHP text_errors
     * Outputs errors in a nice text format.
     * 
     * @param array $errors
     */
    public static function text_errors($errors = null) {
        if($errors) {
            $errorsText = '';
            foreach ($errors as $error) {
                $errorsText .= $error['name'];
            }
            return($errorsText);
        }
    }

    /**
     * SafePHP error_handler
     * SafePHP's error handler.
     * 
     * @param int $errno
     * @param string $errstr
     * @param string $errfile
     * @param int $errline
     * @param array $errcontext
     * @return boolean
     */
    public static function error_handler($errno, $errstr, $errfile = '', $errline = 0, $errcontext = array()) {
        @trigger_error($errstr);
        return TRUE;
    }

    /**
     * SafePHP fatal_error_handler
     * Handles fatal errors in a safe, noncrashy helpful way.
     * 
     * @return exit
     */
    public static function fatal_error_handler() {
        $error = error_get_last();
        if ($error !== NULL) {
            echo $error['message'];
        }
    }

    /**
     * SafePHP file_get_contents
     * Replaces PHP's default file_get_contents with something more secure.
     * 
     * @param string $url
     * @return string
     */
    public static function safe_file_get_contents($url) {

        try {
            $request = Requests::get($url);
            return($request->body);
        } catch (Requests_Exception $e) {
            trigger_error($e->getMessage());
        }
    }

}
