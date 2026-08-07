import { createRouter, createWebHistory } from "vue-router";
import { unauthorized } from "@/net";


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "welcome",
      component: () => import("@/views/welcomeView.vue"),
      children: [
        {
          path: "/",
          name: "welcome-login",
          component: () => import("@/views/welcome/loginPage.vue"),
        },
      ],
    },
    {
      path: "/index",
      name: "index",
      component: () => import("@/views/IndexView.vue"),
    }
  ],
});

router.beforeEach((to) => {
  const isAuthenticated = unauthorized();
  if (to.name.startsWith("welcome-") && !isAuthenticated) {
    return "/index";
  } else if (to.fullPath.startsWith("/index") && isAuthenticated) {
    return "/";
  } else {
    return true;
  }
})

export default router;
