#include <ButtonConstants.au3>
#include <EditConstants.au3>
#include <GUIConstantsEx.au3>
#include <StaticConstants.au3>
#include <WindowsConstants.au3>
#include <File.au3>
#include <Date.au3>

; ================= [ 설정 경로 ] =================
Global Const $sConfigDir  = "C:\HRD_Attendance"
Global Const $sSettingIni = $sConfigDir & "\settings.ini"
Global Const $sCacheIni   = $sConfigDir & "\cache.ini"

; ================= [ 기본값 ] =================
Global Const $sDefaultApi = "http://woori10-0.iptime.org:4402/coolapi/write"
Global Const $sDefaultLog = "C:\HRD_NICE\log"

; ================= [ 전역 변수 ] =================
Global $sApiUrl        = $sDefaultApi
Global $sLogDir        = $sDefaultLog
Global $bIsRunning     = False
Global $sCurrentDate   = ""
Global $sTargetFile    = ""
Global $hFile          = -1
Global $iLastPos       = 0
Global $iCheckInterval = 1000

; 상태머신: 출결저장 처리 라인 버퍼
Global $sPendingLine   = ""
; 보류 중인 트랜잭션(출결저장 라인)의 파일 시작 위치 — 전송 실패 시 여기로 되감아 재시도
Global $iPendingLinePos = 0

; 통계 카운터
Global $iReadCount  = 0
Global $iSendCount  = 0

; COM 에러 메시지 보관 (마지막 전송 실패 사유)
Global $sLastComError = ""

; 콘솔 로그 버퍼 — 무한 누적 방지(상한 초과 시 오래된 줄 잘라냄)
Global $sConsoleBuf = ""
Global $iLogCount   = 0
Global Const $iMaxLogLines = 400

; ================= [ COM 에러 핸들러 ] =================
; WinHTTP .Send() 가 전송 계층에서 실패하면(서버 다운/네트워크 끊김) COM 예외가
; 발생한다. 핸들러가 없으면 스크립트가 그 자리에서 종료되어 밤사이 에이전트가
; 죽고, 다음 날 새 파일이 생겨도 읽지 못한다. 반드시 전역으로 등록한다.
Global $oComError = ObjEvent("AutoIt.Error", "_ComErrFunc")

; ================= [ 설정 폴더 보장 ] =================
If Not FileExists($sConfigDir) Then DirCreate($sConfigDir)

; ================= [ 설정 로드 ] =================
$sApiUrl = IniRead($sSettingIni, "Settings", "ApiUrl", $sDefaultApi)
$sLogDir = IniRead($sSettingIni, "Settings", "LogDir", $sDefaultLog)

; ================= [ GUI 생성 ] =================
Global $hMainGUI = GUICreate("Woori Attendance Agent v2.2", 540, 530, -1, -1)
GUISetBkColor(0xF5F5F7)

; ── 설정 그룹 ──────────────────────────────────────────────
GUICtrlCreateGroup(" 서버 및 경로 설정 ", 10, 10, 520, 115)

GUICtrlCreateLabel("API 주소:", 25, 40, 65, 20)
Global $idInputApi = GUICtrlCreateInput($sApiUrl, 95, 37, 420, 22)

GUICtrlCreateLabel("로그 폴더:", 25, 78, 65, 20)
Global $idInputPath  = GUICtrlCreateInput($sLogDir, 95, 75, 330, 22)
Global $idBtnBrowse  = GUICtrlCreateButton("찾아보기", 432, 74, 83, 24)
Global $idBtnSaveCfg = GUICtrlCreateButton("설정저장", 432, 37, 83, 24)

GUICtrlCreateGroup("", -99, -99, 1, 1)

; ── 제어 버튼 ──────────────────────────────────────────────
Global $idBtnStart = GUICtrlCreateButton("감시 시작", 10, 135, 255, 42)
GUICtrlSetFont($idBtnStart, 11, 800)
Global $idBtnStop  = GUICtrlCreateButton("감시 중지", 275, 135, 255, 42)
GUICtrlSetFont($idBtnStop, 11, 800)
GUICtrlSetState($idBtnStop, $GUI_DISABLE)

; ── 캐시 삭제 버튼 ─────────────────────────────────────────
Global $idBtnClearCache = GUICtrlCreateButton("⟳  캐시 삭제 (처음부터 재전송)", 10, 185, 520, 24)
GUICtrlSetFont($idBtnClearCache, 9, 400)

; ── 진단 버튼 ──────────────────────────────────────────────
Global $idBtnPreview = GUICtrlCreateButton("📋  파일 미리보기 (처음 20줄)", 10, 213, 255, 24)
GUICtrlSetFont($idBtnPreview, 9, 400)
Global $idBtnTest    = GUICtrlCreateButton("📡  API 연결 테스트", 275, 213, 255, 24)
GUICtrlSetFont($idBtnTest, 9, 400)

; ── 콘솔 ───────────────────────────────────────────────────
Global $idLblConsole = GUICtrlCreateLabel("실시간 전송 현황", 15, 249, 350, 20)
GUICtrlSetFont($idLblConsole, 10, 600)
Global $idLblStats = GUICtrlCreateLabel("읽음: 0  /  전송: 0", 370, 249, 160, 20)
GUICtrlSetFont($idLblStats, 9, 400)

Global $idEditConsole = GUICtrlCreateEdit("", 10, 270, 520, 250, _
    BitOR($ES_AUTOVSCROLL, $WS_VSCROLL, $ES_READONLY))
GUICtrlSetBkColor($idEditConsole, 0xFFFFFF)

GUISetState(@SW_SHOW)

; ================= [ 메인 이벤트 루프 ] =================
While 1
    Local $nMsg = GUIGetMsg()
    Switch $nMsg
        Case $GUI_EVENT_CLOSE
            _CleanUp()
            Exit

        Case $idBtnBrowse
            Local $sDir = FileSelectFolder("출결 로그 폴더 선택", "", 1, $sLogDir, $hMainGUI)
            If Not @error Then
                $sLogDir = $sDir
                GUICtrlSetData($idInputPath, $sLogDir)
            EndIf

        Case $idBtnSaveCfg
            $sApiUrl = GUICtrlRead($idInputApi)
            $sLogDir = GUICtrlRead($idInputPath)
            _SaveSettings()
            _LogWrite("[설정] 저장 완료")

        Case $idBtnStart
            $sApiUrl = GUICtrlRead($idInputApi)
            $sLogDir = GUICtrlRead($idInputPath)
            _SaveSettings()

            GUICtrlSetState($idInputApi,   $GUI_DISABLE)
            GUICtrlSetState($idInputPath,  $GUI_DISABLE)
            GUICtrlSetState($idBtnBrowse,  $GUI_DISABLE)
            GUICtrlSetState($idBtnSaveCfg, $GUI_DISABLE)
            GUICtrlSetState($idBtnStart,   $GUI_DISABLE)
            GUICtrlSetState($idBtnStop,    $GUI_ENABLE)

            $bIsRunning  = True
            $sPendingLine = ""
            $iReadCount  = 0
            $iSendCount  = 0
            _UpdateStats()
            _LogWrite("[시스템] 감시 시작 — API: " & $sApiUrl)
            _InitializeFile()

        Case $idBtnStop
            _CleanUp()
            _LogWrite("[시스템] 감시 중지")

            GUICtrlSetState($idInputApi,   $GUI_ENABLE)
            GUICtrlSetState($idInputPath,  $GUI_ENABLE)
            GUICtrlSetState($idBtnBrowse,  $GUI_ENABLE)
            GUICtrlSetState($idBtnSaveCfg, $GUI_ENABLE)
            GUICtrlSetState($idBtnStart,   $GUI_ENABLE)
            GUICtrlSetState($idBtnStop,    $GUI_DISABLE)

            $bIsRunning = False

        Case $idBtnClearCache
            _ClearCache()

        Case $idBtnPreview
            _PreviewFile()

        Case $idBtnTest
            _TestApi()

    EndSwitch

    If $bIsRunning Then _MonitorLogFile()
    Sleep(10)
WEnd

; ================= [ 파일 미리보기 ] =================
; 현재 설정된 경로의 오늘 파일을 열어 처음 20줄 표시
Func _PreviewFile()
    Local $sTmpDir  = GUICtrlRead($idInputPath)
    Local $sDate    = @YEAR & "_" & @MON & @MDAY
    Local $sFile    = $sTmpDir & "\" & $sDate & ".Txt"

    _LogWrite("[미리보기] 대상: " & $sFile)

    If Not FileExists($sFile) Then
        _LogWrite("[미리보기] ❌ 파일이 존재하지 않습니다!")
        _LogWrite("[미리보기]    경로를 확인하세요: " & $sTmpDir)
        Return
    EndIf

    Local $iSize = FileGetSize($sFile)
    _LogWrite("[미리보기] ✔ 파일 발견 — 크기: " & $iSize & " bytes")

    Local $hF = FileOpen($sFile, 0)   ; ANSI(CP949) — HRD NICE는 EUC-KR 출력
    If $hF = -1 Then
        _LogWrite("[미리보기] ❌ 파일 열기 실패 (다른 프로그램이 잠금 중?)")
        Return
    EndIf

    Local $iCount = 0
    While $iCount < 20
        Local $sLine = FileReadLine($hF)
        If @error = -1 Then ExitLoop
        $sLine = StringStripWS($sLine, 3)
        If StringLen($sLine) > 0 Then
            _LogWrite("[미리보기L" & ($iCount + 1) & "] " & $sLine)
            $iCount += 1
        EndIf
    WEnd
    FileClose($hF)
    _LogWrite("[미리보기] 완료 — " & $iCount & "줄 표시")
EndFunc

; ================= [ API 연결 테스트 ] =================
; 테스트 데이터를 API 서버에 직접 전송하여 연결 확인
Func _TestApi()
    Local $sUrl = GUICtrlRead($idInputApi)
    _LogWrite("[테스트] POST → " & $sUrl)

    Local $oHttp = ObjCreate("winhttp.winhttprequest.5.1")
    If Not IsObj($oHttp) Then
        _LogWrite("[테스트] ❌ HTTP 객체 생성 실패")
        Return
    EndIf

    $oHttp.SetTimeouts(5000, 5000, 5000, 10000)

    ; 실제 형식과 동일한 테스트 페이로드 (출결전송 선택 = card||date||AIG||course||memberNo||name||입실||퇴실||회차)
    Local $sTestData = "출결전송 선택 : TEST_CARD||20260601||AIG_TEST||TEST_COURSE||TEST_NO||테스트학생||0900||0000||99"
    $sLastComError = ""
    $oHttp.Open("POST", $sUrl, False)
    $oHttp.SetRequestHeader("Content-Type", "text/plain; charset=utf-8")
    $oHttp.Send(StringToBinary($sTestData, 4))

    If $sLastComError <> "" Then
        _LogWrite("[테스트] ❌ 연결 실패(네트워크) — " & $sLastComError)
        Return
    EndIf

    Local $iStatus = $oHttp.Status
    Local $sResp   = $oHttp.ResponseText

    If $iStatus = 200 Then
        _LogWrite("[테스트] ✔ 서버 응답 200 OK — " & $sResp)
    Else
        _LogWrite("[테스트] ❌ 서버 응답 " & $iStatus & " — " & $sResp)
    EndIf
EndFunc

; ================= [ 캐시 삭제 ] =================
Func _ClearCache()
    IniWrite($sCacheIni, "Cache", "Date",    "")
    IniWrite($sCacheIni, "Cache", "FilePos", "0")

    If $bIsRunning Then
        If $hFile <> -1 Then
            FileClose($hFile)
            $hFile = -1
        EndIf
        $iLastPos = 0
        $sPendingLine = ""
        $iReadCount   = 0
        $iSendCount   = 0
        _UpdateStats()
        _LogWrite("[캐시] 삭제 완료 — 처음부터 재전송 시작 (중복은 서버가 자동 skip)")
    Else
        _LogWrite("[캐시] 삭제 완료 — 다음 감시 시작 시 처음부터 전송")
    EndIf
EndFunc

; ================= [ 설정 저장 ] =================
Func _SaveSettings()
    IniWrite($sSettingIni, "Settings", "ApiUrl", $sApiUrl)
    IniWrite($sSettingIni, "Settings", "LogDir", $sLogDir)
EndFunc

; ================= [ 파일 초기화 ] =================
Func _InitializeFile()
    If $hFile <> -1 Then FileClose($hFile)
    $hFile = -1
    $sPendingLine = ""

    $sCurrentDate = @YEAR & "_" & @MON & @MDAY
    $sTargetFile  = $sLogDir & "\" & $sCurrentDate & ".Txt"

    Local $sCacheDate = IniRead($sCacheIni, "Cache", "Date", "")
    Local $iCachePos  = Int(IniRead($sCacheIni, "Cache", "FilePos", "0"))

    If $sCacheDate = $sCurrentDate And $iCachePos > 0 Then
        $iLastPos = $iCachePos
        _LogWrite("[캐시] 오늘 캐시 발견 — 위치 " & $iLastPos & " 부터 재개")
    Else
        $iLastPos = 0
        _LogWrite("[안내] 오늘 캐시 없음 — 처음부터 전체 스캔 (중복은 서버가 자동 skip)")
    EndIf

    _LogWrite("[안내] 감시 파일: " & $sTargetFile)
    If Not FileExists($sTargetFile) Then
        _LogWrite("[경고] 아직 파일 없음 — HRD 프로그램 실행 후 생성되면 자동 감지")
    EndIf
EndFunc

; ================= [ 파일 감시 루프 ] =================
Func _MonitorLogFile()
    Static $iTimer = 0
    If TimerDiff($iTimer) < $iCheckInterval Then Return
    $iTimer = TimerInit()

    If $sCurrentDate <> @YEAR & "_" & @MON & @MDAY Then
        _LogWrite("[시스템] 날짜 변경 — 새 파일로 전환")
        IniWrite($sCacheIni, "Cache", "Date", "")
        IniWrite($sCacheIni, "Cache", "FilePos", "0")
        _InitializeFile()
    EndIf

    If Not FileExists($sTargetFile) Then
        If $hFile <> -1 Then
            FileClose($hFile)
            $hFile = -1
            _LogWrite("[경고] 로그 파일 없음: " & $sTargetFile)
        EndIf
        Return
    EndIf

    If $hFile = -1 Then
        $hFile = FileOpen($sTargetFile, 0)   ; ANSI(CP949) — HRD NICE는 EUC-KR 출력
        If $hFile = -1 Then
            _LogWrite("[에러] 파일 열기 실패: " & $sTargetFile)
            Return
        EndIf
        FileSetPos($hFile, $iLastPos, 0)
        _LogWrite("[안내] 파일 열림 — " & $sCurrentDate & ".Txt  (pos=" & $iLastPos & ")")
    EndIf

    Local $iCurrentSize = FileGetSize($sTargetFile)

    If $iCurrentSize < $iLastPos Then
        FileSetPos($hFile, 0, 0)
        $iLastPos = 0
        _LogWrite("[안내] 파일 초기화 감지 — 처음부터 재스캔")
    EndIf

    If $iCurrentSize > $iLastPos Then
        FileSetPos($hFile, $iLastPos, 0)

        ; $iSafePos = 커밋해도 되는 위치. 정상 처리(버퍼링/전송성공/폐기/무관)는 계속
        ; 전진하지만, 전송이 네트워크로 실패하면 보류 트랜잭션(출결저장 라인)으로
        ; 되감아 다음 주기에 그 줄부터 다시 읽어 재전송한다. (줄 누락 방지)
        Local $iSafePos = $iLastPos
        While 1
            Local $iLinePos = FileGetPos($hFile)
            Local $sLine = FileReadLine($hFile)
            If @error = -1 Then ExitLoop

            Local $iRet = _ProcessLine($sLine)
            If $iRet = -1 Then
                ; 전송 실패 → 출결저장 라인 위치로 되감고 중단(다음 주기 재시도)
                $iSafePos = $iPendingLinePos
                ExitLoop
            EndIf
            ; 출결저장 라인은 전송 실패 시 복귀점으로 위치를 기억해 둔다
            If $iRet = 1 Then $iPendingLinePos = $iLinePos
            $iSafePos = FileGetPos($hFile)
        WEnd

        $iLastPos = $iSafePos
        FileSetPos($hFile, $iLastPos, 0)

        IniWrite($sCacheIni, "Cache", "Date",    $sCurrentDate)
        IniWrite($sCacheIni, "Cache", "FilePos", $iLastPos)
    EndIf
EndFunc

; ================= [ 라인 처리 상태머신 ] =================
;  "출결전송 선택" → 버퍼링 (AIG·회차 포함된 풍부한 라인: card||date||AIG||course||
;                    memberNo||name||입실||퇴실||회차)
;  "출결전송 처리 : 정상" → 버퍼된 라인 전송 / 비정상 → 폐기
;  ※ 과거엔 "출결저장 처리"(회차 없음)를 보냈으나, 같은 과정의 오전/오후반을
;     회차로 구분하기 위해 "출결전송 선택" 라인으로 변경.
;  반환값:  1 = 버퍼링됨(미완 트랜잭션 시작)
;          -1 = 전송 시도했으나 네트워크 실패(되감아 재시도)
;           0 = 무관 라인 / 전송 성공 / 폐기(커밋 가능)
Func _ProcessLine($sLine)
    ; ── 출결 이벤트(입실/퇴실): AIG·회차가 들어있는 "전송 선택" 라인을 버퍼 ──
    If StringInStr($sLine, "전송 선택") Then
        $sPendingLine = $sLine
        $iReadCount  += 1
        _UpdateStats()
        Return 1

    ; ── 전송 결과 ──────────────────────────────────────────────
    ElseIf StringInStr($sLine, "전송 처리") Then
        If StringInStr($sLine, "정상") And $sPendingLine <> "" Then
            _LogWrite("[전송] " & $sPendingLine)
            If _SendToApi($sPendingLine) Then
                $iSendCount += 1
                _UpdateStats()
                $sPendingLine = ""
                Return 0
            Else
                ; 전송 실패 → 버퍼 유지, 되감아 재시도 (줄 누락 방지)
                Return -1
            EndIf
        ElseIf Not StringInStr($sLine, "정상") And $sPendingLine <> "" Then
            _LogWrite("[폐기] 전송 비정상 → " & $sPendingLine)
        EndIf
        $sPendingLine = ""
    EndIf

    Return 0
EndFunc

; ================= [ REST API 전송 ] =================
Func _SendToApi($sData)
    Local $oHttp = ObjCreate("winhttp.winhttprequest.5.1")
    If Not IsObj($oHttp) Then
        _LogWrite("[에러] HTTP 객체 생성 실패")
        Return False
    EndIf

    ; resolve / connect / send / receive 타임아웃(ms) — GUI 루프가 멈추지 않도록
    $oHttp.SetTimeouts(5000, 5000, 5000, 10000)

    $sLastComError = ""
    $oHttp.Open("POST", $sApiUrl, False)
    $oHttp.SetRequestHeader("Content-Type", "text/plain; charset=utf-8")
    $oHttp.Send(StringToBinary($sData, 4))

    ; 전송 계층 실패(서버 다운/타임아웃)는 COM 에러 핸들러가 $sLastComError 에 기록.
    ; 여기서 스크립트가 죽지 않고 False 만 반환하므로 감시 루프는 계속 살아 있다.
    If $sLastComError <> "" Then
        _LogWrite("[에러] 전송 실패(네트워크) → " & $sLastComError)
        Return False
    EndIf

    If $oHttp.Status = 200 Then
        Return True
    Else
        _LogWrite("[에러] HTTP " & $oHttp.Status & " ← " & $sData)
        Return False
    EndIf
EndFunc

; ================= [ 파일 핸들 정리 ] =================
Func _CleanUp()
    If $hFile <> -1 Then
        FileClose($hFile)
        $hFile = -1
    EndIf
    $sPendingLine = ""
EndFunc

; ================= [ 통계 업데이트 ] =================
Func _UpdateStats()
    GUICtrlSetData($idLblStats, "읽음: " & $iReadCount & "  /  전송: " & $iSendCount)
EndFunc

; ================= [ 콘솔 로그 출력 ] =================
; 내부 문자열 버퍼에 누적하되, $iMaxLogLines 초과 시 최근 절반만 유지한다.
; (장시간 구동 시 Edit 컨트롤 무한 증가로 인한 메모리/속도 저하 방지)
Func _LogWrite($sMessage)
    Local $sLine = "[" & @HOUR & ":" & @MIN & ":" & @SEC & "] " & $sMessage

    If $sConsoleBuf = "" Then
        $sConsoleBuf = $sLine
    Else
        $sConsoleBuf = $sConsoleBuf & @CRLF & $sLine
    EndIf
    $iLogCount += 1

    If $iLogCount > $iMaxLogLines Then
        Local $aL = StringSplit($sConsoleBuf, @CRLF, 2)   ; 2=$STR_ENTIRESPLIT
        Local $iKeep  = Int($iMaxLogLines / 2)
        Local $iStart = $aL[0] - $iKeep + 1
        If $iStart < 1 Then $iStart = 1
        Local $sTrim = $aL[$iStart]
        For $i = $iStart + 1 To $aL[0]
            $sTrim = $sTrim & @CRLF & $aL[$i]
        Next
        $sConsoleBuf = $sTrim
        $iLogCount   = $aL[0] - $iStart + 1
    EndIf

    GUICtrlSetData($idEditConsole, $sConsoleBuf)
    GUICtrlSendMsg($idEditConsole, $EM_LINESCROLL, 0, 100000)
EndFunc

; ================= [ COM 에러 핸들러 ] =================
; .Send() 등 COM 호출이 실패할 때 호출된다. 여기서 처리(=무시하고 기록)하면
; 스크립트가 종료되지 않고 계속 실행된다. 사유는 $sLastComError 로 전달.
Func _ComErrFunc($oError)
    $sLastComError = "0x" & Hex($oError.number, 8) & " " & $oError.description
EndFunc
