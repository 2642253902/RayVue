import axios from "axios";
import { ElMessage } from "element-plus";

const authItemName = "access_token";

const defaultFailure = (message, code, url) => {
  console.warn(
    "请求地址：" +
    url +
    "，请求失败，错误码：" +
    code +
    "，错误信息：" +
    message,
  );
  ElMessage.error(message);
};

function takeAccessToken() {
  const str =
    localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName);
  if (!str) {
    return null;
  }
  const authObj = JSON.parse(str);
  if (authObj.expire <= new Date().getTime()) {
    deleteAccessToken();
    ElMessage.error("登录已过期，请重新登录");
    return null;
  }
  return authObj.token;
}

function storeAccessToken(token, remember, expire) {
  const authObj = {
    token: token,
    expire: expire,
  };
  const str = JSON.stringify(authObj);
  if (remember) {
    localStorage.setItem(authItemName, str);
  } else {
    sessionStorage.setItem(authItemName, str);
  }
}

function deleteAccessToken() {
  localStorage.removeItem(authItemName);
  sessionStorage.removeItem(authItemName);
}

const defaultError = (err) => {
  console.error(err);
  ElMessage.error("请求失败，请检查网络连接或联系管理员");
};

function internalPost(url, data, headers, success, failure, error = defaultError) {
  axios.post(url, data, { headers: headers }).then(({ data }) => {
    if (data.code === 200) {
      success(data.data)
    } else if (data.code === 401) {
      failure(data.message, data.code, url);
    } else {
      failure(data.message, data.code, url)
    }
  }).catch(err => error(err))
}

function accessTokenHeader() {
  const token = takeAccessToken()
  return token ? {
    "Authorization": "Bearer " + takeAccessToken()
  } : {}
}

function internalGet(url, header, success, failure, error = defaultError) {
  axios.get(url, { headers: header }).then(({ data }) => {
    if (data.code === 200) {
      success(data.data)
    } else {
      failure(data.message, data.code, url)
    }
  }).catch(err => error(err))
}

function get(url, success, failure = defaultFailure) {
  internalGet(url, accessTokenHeader(), success, failure)
}

function post(url, data, success, failure = defaultFailure) {
  internalPost(url, data, accessTokenHeader(), success, failure)
}

function login(username, password, remember, success, failure = defaultFailure,
) {
  internalPost("/api/auth/login",
    {
      username: username,
      password: password,
      remember: remember,
    },
    {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    (data) => {
      storeAccessToken(data.token, remember, data.expireTime);
      ElMessage.success("登录成功,欢迎" + data.username + "回来");
      success(data);
    },
    failure
  );
}

function logout(success, failure = defaultFailure) {
  get("/api/auth/logout", () => {
    deleteAccessToken();
    ElMessage.success("注销成功");
    success();
  }, failure);
}

function unauthorized() {
  return !takeAccessToken();
}

export { login, logout, get, post, unauthorized };
