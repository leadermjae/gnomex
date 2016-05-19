<%@ page language="java" contentType="text/html; ISO-8859-1" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>

<body onload="setFocus()">

<c:choose>
    <c:when test="${isPublicExperiment}">
        <script type="text/javascript">
            window.location = "gnomexGuestFlex.jsp?requestNumber=${requestNumber}";
        </script>
    </c:when>
    <c:when test="${isPublicAnalysis}">
        <script type="text/javascript">
            window.location = "gnomexGuestFlex.jsp?analysisNumber=${analysisNumber}";
        </script>
    </c:when>
    <c:when test="${isPublicDataTrack}">
        <script type="text/javascript">
            window.location = "gnomexGuestFlex.jsp?dataTrackNumber=${dataTrackNumber}";
        </script>
    </c:when>
    <c:when test="${isPublicTopic}">
        <script type="text/javascript">
            window.location = "gnomexGuestFlex.jsp?topicNumber=${topicNumber}";
        </script>
    </c:when>
    <c:otherwise></c:otherwise>
</c:choose>

<head>
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="0">

    <link rel="stylesheet" href="resources/css/bootstrap.min.css">
    <link rel="stylesheet" href="css/login.css" type="text/css"/>

    <title>Sign in to GNomEx</title>

    <script type="text/javascript">
        function setFocus() {
            theform.username.focus();
        }
    </script>
</head>

<div class="container login-container">

    <form class="form-signin clearfix" id="theform" method="POST" action="j_security_check${idCoreParm}">

        <div class="header-bar">
            <img class="header-logo" src="images/svg/gnomex.svg"/>
            <span class="pull-right header-title">Sign In</span>
        </div>
        <c:if test="${itemNotPublic}">
            <div class="topPanel">
                The ${itemType} you are linking to does not have public visibility. Please sign in to proceed:
            </div>
        </c:if>

        <label for="username" class="sr-only">User name</label>
        <input id="username" type="text" class="form-control" name="j_username" placeholder="User name" title="Enter your username here">

        <label for="password" class="sr-only">Password</label>
        <input id="password" type="password" class="form-control" name="j_password" placeholder="Password" title="Enter your password here">

        <div class="form-signin-button-block">
            <div class="info-block">
                <%--<c:if test="${showCampusInfoLink}">--%>
                    <%--University of Utah investigators should sign in with their UNID and CIS password.--%>
                <%--</c:if>--%>
            </div>
            <div class="button-block">
                <input class="btn btn-primary btn-block" type="submit" value="Sign in" title="Click here to Sign In">
            </div>
        </div>
    </form>
    <div class="form-signin-secondary-button-block btn-group btn-group-sm">
        <c:if test="${allowGuest}">
            <!-- Note that guest ignores idCore parameter -- guest just sees all public objects. -->
            <a href="gnomexGuestFlex.jsp" class="sec-button btn btn-sm btn-link" title="Click here to login as a guest">Guest Login</a>
        </c:if>
        <a href="reset_password${idCoreParm}" class="sec-button btn btn-sm btn-link" title="Click here to reset your password">Reset Password</a>
        <c:if test="${showUserSignup}">
            <a href="select_core.jsp${idCoreParm}" class="sec-button btn btn-sm btn-link" title="Click here to register a new account">New Account</a>
        </c:if>
    </div>
    <c:if test="${showCampusInfoLink}">
    <div class="well well-sm campus-info clearfix">
        University of Utah investigators should sign in with their UNID and CIS password.
    </div>
    </c:if>
    <c:if test="${errFlag.equals('Y')}">
        <div id="error" class="alert alert-danger" role="alert">The <strong>user name</strong> or <strong>password</strong> you entered may be incorrect. Please try again.</div>
    </c:if>
</div>


</div>
</body>
</html>