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

; 통계 카운터
Global $iReadCount  = 0
Global $iSendCount  = 0

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

    ; 실제 형식과 동일한 테스트 페이로드 (서버에서 TEST 과정명으로 처리됨)
    Local $sTestData = "출결저장 처리 : TEST_CARD||테스트학생||TEST_COURSE||0900||0000"
    $oHttp.Open("POST", $sUrl, False)
    $oHttp.SetRequestHeader("Content-Type", "text/plain; charset=utf-8")
    $oHttp.Send(StringToBinary($sTestData, 4))

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
        While 1
            Local $sLine = FileReadLine($hFile)
            If @error = -1 Then ExitLoop
            _ProcessLine($sLine)
        WEnd
        $iLastPos = FileGetPos($hFile)

        IniWrite($sCacheIni, "Cache", "Date",    $sCurrentDate)
        IniWrite($sCacheIni, "Cache", "FilePos", $iLastPos)
    EndIf
EndFunc

; ================= [ 라인 처리 상태머신 ] =================
;  "출결저장 처리" → 버퍼링
;  "출결전송 처리 : 정상" → 버퍼된 라인 전송
;  기타 출결전송 처리(에러 등) → 버퍼 폐기
Func _ProcessLine($sLine)
    If StringInStr($sLine, "출결저장 처리") Then
        $sPendingLine = $sLine
        $iReadCount  += 1
        _UpdateStats()

    ElseIf StringInStr($sLine, "출결전송 처리") Then
        If StringInStr($sLine, "정상") And $sPendingLine <> "" Then
            _LogWrite("[전송] " & $sPendingLine)
            If _SendToApi($sPendingLine) Then
                $iSendCount += 1
                _UpdateStats()
            EndIf
        ElseIf Not StringInStr($sLine, "정상") And $sPendingLine <> "" Then
            _LogWrite("[폐기] 출결전송 비정상 → " & $sPendingLine)
        EndIf
        $sPendingLine = ""
    EndIf
EndFunc

; ================= [ REST API 전송 ] =================
Func _SendToApi($sData)
    Local $oHttp = ObjCreate("winhttp.winhttprequest.5.1")
    If Not IsObj($oHttp) Then
        _LogWrite("[에러] HTTP 객체 생성 실패")
        Return False
    EndIf

    $oHttp.Open("POST", $sApiUrl, False)
    $oHttp.SetRequestHeader("Content-Type", "text/plain; charset=utf-8")
    $oHttp.Send(StringToBinary($sData, 4))

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
Func _LogWrite($sMessage)
    Local $sTime    = "[" & @HOUR & ":" & @MIN & ":" & @SEC & "] "
    Local $sCurrent = GUICtrlRead($idEditConsole)
    Local $sNew     = $sTime & $sMessage

    If $sCurrent = "" Then
        GUICtrlSetData($idEditConsole, $sNew)
    Else
        GUICtrlSetData($idEditConsole, $sCurrent & @CRLF & $sNew)
    EndIf

    GUICtrlSendMsg($idEditConsole, $EM_LINESCROLL, 0, 100000)
EndFunc
