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

function internalPost(
  url,
  data,
  header,
  success,
  failure,
  error = defaultError,
) {
  axios
    .post(url, data, { headers: header })
    .then((data) => {
      if (data.status === 200) {
        success(data.data);
      } else {
        failure(data.message, data.code, url);
      }
    })
    .catch((err) => {
      error(err);
    });
}

function internalGet(url, header, success, failure, error = defaultError) {
  axios
    .get(url, { headers: header })
    .then((data) => {
      if (data.status === 200) {
        success(data.data);
      } else {
        failure(data.message, data.code, url);
      }
    })
    .catch((err) => {
      error(err);
    });
}

function login(
  username,
  password,
  remember,
  success,
  failure = defaultFailure,
) {
  (internalPost("/api/auth/login"),
    {
      username: username,
      password: password,
      remember: remember,
    },
    {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    (data) => {
      storeAccessToken(data.token, remember, data.expire);
      ElMessage.success("登录成功,欢迎" + data.username + "回来");
      success(data);
    },
    failure);
}

export { login };
