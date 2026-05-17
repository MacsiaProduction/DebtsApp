import http from "k6/http";
import { check, sleep } from "k6";

function loadStages() {
  const defaultStages = [
    { duration: "30s", target: 10 },
    { duration: "60s", target: 40 },
    { duration: "60s", target: 80 },
    { duration: "30s", target: 0 }
  ];

  if (!__ENV.K6_STAGES) {
    return defaultStages;
  }

  return __ENV.K6_STAGES.split(",").map((stage) => {
    const [duration, target] = stage.split(":");
    return {
      duration: duration.trim(),
      target: Number(target.trim())
    };
  });
}

export const options = {
  stages: loadStages(),
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"]
  }
};

const baseUrl = __ENV.BASE_URL || "https://debtsapp.example.com";

export default function () {
  const response = http.get(`${baseUrl}/api/session`);

  check(response, {
    "session endpoint returns 200": (r) => r.status === 200,
    "session token is not empty": (r) => !!r.body && r.body.length > 10
  });

  sleep(1);
}
