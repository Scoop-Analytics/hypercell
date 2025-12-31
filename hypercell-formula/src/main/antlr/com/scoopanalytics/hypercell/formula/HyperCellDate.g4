grammar HyperCellDate;

@header {
    package com.scoopanalytics.hypercell.formula;
}

start
:
        monthname ' ' day (',' ' ' (year | shortyear)) (','? ' '* time)?
        | year datepartsep month datepartsep day (datetimesep time)?
        | day datepartsep monthname (datepartsep (year | shortyear))? (datetimesep time)?
        | month datepartsep day datepartsep (year | shortyear) (datetimesep time)?
        | monthname ' ' (year | shortyear)
;

time
:
        hour ':' min (':' sec)? (' '* ampm)? ' '* timezone?
;

year
:
        DIGIT DIGIT DIGIT DIGIT
;

shortyear
:
        DIGIT DIGIT
;

month
:
        DIGIT DIGIT?  // Example of a test that could be used{ getCurrentToken().getText().equals("1")}?
;

monthname
:
        JANTOKEN
        | FEBTOKEN
        | MARTOKEN
        | APRILTOKEN
        | MAYTOKEN
        | JUNETOKEN
        | JULYTOKEN
        | AUGTOKEN
        | SEPTOKEN
        | OCTTOKEN
        | NOVTOKEN
        | DECTOKEN
;

day
:
        DIGIT DIGIT?
;

hour
:
        DIGIT DIGIT?
;

min
:
        DIGIT DIGIT
;

sec
:
        DIGIT DIGIT ('.' DIGIT+)?
;

datepartsep
:
        '-'     | '/' | 'T'
;

ampm:
        (AMTOKEN | PMTOKEN)
;

timezone
:
        TIMEZONE | timezone_uslong | 'Z' | ('+'|'-') DIGIT DIGIT ':'? DIGIT DIGIT
;

TIMEZONE
:
        'ACT' | 'AET' | 'AGT' | 'ART' | 'AST' | 'BET' | 'BST' | 'CAT' | 'CET' | 'CNT' | 'CST' | 'CTT' | 'EAT' | 'ECT' | 'EET' | 'EST' | 'GMT' | 'HST' | 'IET' | 'IST' | 'JST' | 'MDT' | 'MET' | 'MIT' | 'MST' | 'NET' | 'NST' | 'PLT' | 'PNT' | 'PRC' | 'PRT' | 'PST' | 'ROK' | 'SST' | 'UCT' | 'UTC' | 'VST' | 'WET'
;

timezone_uslong
:
    TIMEZONE_PACIFIC | TIMEZONE_MOUNTAIN | TIMEZONE_CENTRAL | TIMEZONE_EASTERN
;

TIMEZONE_PACIFIC:
    '[US/Pacific]'
;
TIMEZONE_MOUNTAIN:
    '[US/Mountain]'
;
TIMEZONE_CENTRAL:
    '[US/Central]'
;
TIMEZONE_EASTERN:
    '[US/Eastern]'
;

datetimesep
:
        ' '+ | 'T' | '_' | (',' ' '*)
;

JANTOKEN
:
        ('J'|'j') ('A'|'a') ('N'|'n')
        (
                ('U'|'u') ('A'|'a') ('R'|'r') ('Y'|'y')
        )?
;

FEBTOKEN
:
        ('F'|'f') ('E'|'e') ('B'|'b')
        (
                ('R'|'r') ('U'|'u') ('A'|'a') ('R'|'r') ('Y'|'y')
        )?
;

MARTOKEN
:
        ('M'|'m') ('A'|'a') ('R'|'r')
        (
                ('C'|'c') ('H'|'h')
        )?
;

APRILTOKEN
:
        ('A'|'a') ('P'|'p') ('R'|'r')
        (
                ('I'|'i') ('L'|'l')
        )?
;

MAYTOKEN
:
        ('M'|'m') ('A'|'a') ('Y'|'y')
;

JUNETOKEN
:
        ('J'|'j') ('U'|'u') ('N'|'n')
        (
                ('E'|'e')
        )?
;

JULYTOKEN
:
        ('J'|'j') ('U'|'u') ('L'|'l')
        (
                ('Y'|'y')
        )?
;

AUGTOKEN
:
        ('A'|'a') ('U'|'u') ('G'|'g')
        (
                ('U'|'u') ('S'|'s') ('T'|'t')
        )?
;

SEPTOKEN
:
        ('S'|'s')('E'|'e') ('P'|'p')
        (
                ('T'|'t') ('E'|'e') ('M'|'m') ('B'|'b') ('E'|'e') ('R'|'r')
        )?
;

OCTTOKEN
:
        ('O'|'o') ('C'|'c') ('T'|'t')
        (
                ('O'|'o') ('B'|'b') ('E'|'e') ('R'|'r')
        )?
;

NOVTOKEN
:
        ('N'|'n') ('O'|'o') ('V'|'v')
        (
                ('E'|'e') ('M'| 'm') ('B'|'b') ('E'|'e') ('R'|'r')
        )?
;

DECTOKEN
:
        ('D'|'d') ('E'|'e') ('C'|'c')
        (
                ('E'|'e') ('M'|'m') ('B'|'b') ('E'|'e') ('R'|'r')
        )?
;

AMTOKEN
:
        ('A'|'a') ('M'|'m')
;

PMTOKEN
:
        ('P'|'p') ('M'|'m')
;

DIGIT
:
        [0-9]
;

fragment
ALPHACAPS
:
        [A-Z]
;
