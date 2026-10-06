output "security_policy_id" {
  value       = google_compute_security_policy.payment_policy.id
  description = "The Cloud Armor security policy ID"
}

output "security_policy_name" {
  value       = google_compute_security_policy.payment_policy.name
  description = "The Cloud Armor security policy name"
}
