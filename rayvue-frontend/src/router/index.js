import { createRouter, createWebHistory } from "vue-router";

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
  ],  
});

export default router;
