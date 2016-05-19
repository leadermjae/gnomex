<%@ page language="java" contentType="text/html; ISO-8859-1" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>

<head>
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="0">

    <link rel="stylesheet" href="resources/css/bootstrap.min.css">
    <link rel="stylesheet" href="css/login.css" type="text/css"/>
    <title>Reset GNomEx Password</title>
    <script type="text/javascript">
        function setFocus() {
            theform.username.focus();
        }

        function showEmail() {

            document.getElementById("byEmail").style.display = "block";
            document.getElementById("byName").style.display = "none";
            document.getElementById("forgotUserName").innerHTML = "Lookup by user name";
            document.getElementById("forgotUserName").onclick = hideEmail;

        }

        function hideEmail() {
            document.getElementById("byEmail").style.display = "none";
            document.getElementById("byName").style.display = "block";
            document.getElementById("forgotUserName").innerHTML = "Lookup by email";
            document.getElementById("forgotUserName").onclick = showEmail;

        }
    </script>
    <style>
        .font-medium {
            font-size: medium;
        }

        #byName, #byEmail {
            margin-bottom: 10px;
        }

    </style>
</head>

<body onload="setFocus(); hideEmail()">

<div class="container login-container">

    <form class="form-signin form-reset clearfix" id="theform" method="POST" action="ChangePassword.gx">

        <div class="header-bar">
            <img class="header-logo" src="images/svg/gnomex.svg"/>
            <span class="pull-right header-title">Reset Password</span>
        </div>

        <%--<h3>Reset Password</h3>--%>

        <div id="byName">
            <label for="username" class="sr-only">User name</label>
            <input id="username" name="userName" type="text" class="form-control" placeholder="User name"/>
        </div>

        <div id="byEmail">
            <label for="email" class="sr-only">Email</label>
            <input id="email" name="email" type="email" class="form-control" placeholder="Email"/>
        </div>

        <div class="form-signin-button-block clearfix">
            <div class="info-block font-medium">
                <a href="#" onclick="showEmail()" class="btn btn-link" id="forgotUserName">Lookup by email</a>
            </div>
            <div class="button-block">
                <input type="submit" class="btn btn-primary btn-block" value="Submit"/>
            </div>
        </div>

        <input type="hidden" name="responsePageSuccess" value="/reset_password_success.jsp${idCoreParm}"/>
        <input type="hidden" name="responsePageError" value="/reset_password.jsp${idCoreParm}"/>
        <input type="hidden" name="idCoreParm" value="${idCoreParm}"/>
    </form>
    <div class="form-signin-secondary-button-block btn-group btn-group-sm">
        <a class="btn btn-sm btn-link" href="gnomexFlex.jsp${idCoreParm}" title="Click here to sign in">Sign in</a>
        <c:if test="${showUserSignup}">
            <a href="select_core.jsp${idCoreParm}" class="btn btn-sm btn-link"
               title="Click here to register a new account">New Account</a>
        </c:if>
    </div>
    <c:if test="${showCampusInfoLink}">
        <div class="well well-sm campus-info clearfix">
            If you have registered using your uNID (u00000000), your password is tied to the University Campus
            Information System. Please use the <a href='https://gate.acs.utah.edu/' class="other" target='_blank'>Campus
            Information System</a> to change or reset your password.
        </div>
    </c:if>
    <c:if test="${not empty message}">
        <div id="error" class="alert alert-warning" role="alert">${message}</div>
    </c:if>
</div>

</div>

</body>
</html>