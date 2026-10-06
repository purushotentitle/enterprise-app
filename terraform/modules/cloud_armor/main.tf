resource "google_compute_security_policy" "payment_policy" {
  name        = "${var.environment}-${var.app_name}-waf-policy"
  description = "Production Cloud Armor WAF Policy for ${var.app_name} with DDoS and rate limiting"

  # Default rule: allow legitimate traffic
  rule {
    action   = "allow"
    priority = "2147483647"
    match {
      versioned_expr = "SRC_IPS_V1"
      config {
        src_ip_ranges = ["*"]
      }
    }
    description = "Default allow"
  }

  # Rate limiting rule: Prevent brute force / card testing attacks on payment APIs
  rule {
    action   = "rate_based_ban"
    priority = "1000"
    match {
      versioned_expr = "SRC_IPS_V1"
      config {
        src_ip_ranges = ["*"]
      }
    }
    rate_limit_options {
      conform_action = "allow"
      exceed_action  = "deny(429)"
      enforce_on_key = "IP"
      rate_limit_threshold {
        count        = 120
        interval_sec = 60
      }
      ban_duration_sec = 300
    }
    description = "Rate limit payments to 120 requests/minute per client IP"
  }

  # OWASP CRS Rule: SQL Injection Defense
  rule {
    action   = "deny(403)"
    priority = "2000"
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('sqli-v33-stable')"
      }
    }
    description = "Block SQL injection attempts"
  }

  # OWASP CRS Rule: Cross-Site Scripting Defense
  rule {
    action   = "deny(403)"
    priority = "3000"
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('xss-v33-stable')"
      }
    }
    description = "Block XSS attempts"
  }

  # OWASP CRS Rule: Remote Code Execution Defense
  rule {
    action   = "deny(403)"
    priority = "4000"
    match {
      expr {
        expression = "evaluatePreconfiguredExpr('rce-v33-stable')"
      }
    }
    description = "Block RCE exploits"
  }
}
