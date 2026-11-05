"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { User, Lock, ArrowRight, Info, MapPin, Phone, ShieldCheck } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<"login" | "about">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [signingIn, setSigningIn] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (signingIn) return;
    setSigningIn(true);
    setLoginError("");
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST", credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password, rememberMe }),
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || "Unable to sign in.");
      setPassword("");
      router.push("/admin/timetable");
    } catch (error) {
      setLoginError(error instanceof Error ? error.message : "Unable to sign in. Please try again.");
    } finally { setSigningIn(false); }
  };

  return (
    <main className="relative min-h-screen overflow-x-hidden font-sans">
      {/* Background with Faculty Campus view */}
      <div className="fixed inset-0 -z-10">
        <Image
          src="/images/faculty-background.jpg"
          alt="Faculty of Engineering, University of Ruhuna"
          fill
          priority
          className="object-cover"
        />
        <div className="absolute inset-0 bg-black/55 backdrop-blur-[2px]" />
      </div>

      {/* Top Header Bar / Navigation */}
      <header className="relative z-10 flex flex-wrap items-center justify-between border-b border-white/15 bg-black/40 px-6 py-3 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <Image
            src="/images/university-logo.png"
            alt="University of Ruhuna Logo"
            width={48}
            height={48}
            className="h-12 w-auto drop-shadow-md"
            priority
          />
          <div>
            <h1 className="text-base sm:text-lg font-bold tracking-wide text-[#f5c04a]">
              Faculty of Engineering
            </h1>
            <p className="text-xs font-medium text-gray-200">
              University of Ruhuna, Sri Lanka
            </p>
          </div>
        </div>

        <nav className="flex items-center gap-2 mt-2 sm:mt-0">
          <button
            type="button"
            onClick={() => setActiveTab("login")}
            className={`rounded-full px-5 py-1.5 text-xs sm:text-sm font-semibold transition ${
              activeTab === "login"
                ? "bg-[#f5c04a] text-[#4b2a0a] shadow-md"
                : "text-gray-200 hover:bg-white/10"
            }`}
          >
            MIS Portal Login
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("about")}
            className={`rounded-full px-5 py-1.5 text-xs sm:text-sm font-semibold transition ${
              activeTab === "about"
                ? "bg-[#f5c04a] text-[#4b2a0a] shadow-md"
                : "text-gray-200 hover:bg-white/10"
            }`}
          >
            About MIS & Faculty
          </button>
        </nav>
      </header>

      {/* Main Content Area */}
      <div className="relative z-10 flex min-h-[calc(100vh-70px)] items-center justify-center p-4 sm:p-8">
        {activeTab === "login" ? (
          <div className="grid w-full max-w-5xl items-center gap-8 lg:grid-cols-2">
            {/* Left Branding - matches official MIS login banner */}
            <div className="hidden lg:flex flex-col items-center justify-center rounded-3xl bg-[#4b2a0a]/85 p-10 text-center shadow-2xl backdrop-blur-md border border-[#f5c04a]/30">
              <Image
                src="/images/university-logo.png"
                alt="University of Ruhuna Logo"
                width={120}
                height={120}
                className="mb-6 h-28 w-auto rounded-full bg-white/10 p-2 shadow-lg"
                priority
              />
              <span className="mb-3 inline-block rounded-full bg-[#f5c04a]/20 px-4 py-1 text-xs font-semibold uppercase tracking-wider text-[#f5c04a]">
                Academic & Administrative Portal
              </span>
              <h1 className="text-4xl font-extrabold leading-snug tracking-tight text-[#f5c04a]">
                Management Information <br /> System
              </h1>
              <div className="my-5 h-1 w-20 rounded-full bg-[#f5c04a]" />
              <p className="text-xl font-bold text-white/95">
                Faculty of Engineering
              </p>
              <p className="text-base text-gray-300">
                University of Ruhuna, Hapugala, Galle
              </p>
              <div className="mt-8 flex items-center gap-2 rounded-xl bg-black/40 px-4 py-2 text-xs text-amber-200/90">
                <ShieldCheck size={16} className="text-[#f5c04a]" />
                Timetable Management & Academic Scheduling System
              </div>
            </div>

            {/* Right Card - matches official MIS login form (f4e7c8 card, f5c04a border) */}
            <div className="mx-auto w-full max-w-md">
              <div className="w-full rounded-3xl border-4 border-[#f5c04a] bg-[#f4e7c8] p-7 sm:p-10 shadow-2xl">
                {/* Mobile header (shown on small screens) */}
                <div className="mb-6 text-center lg:hidden">
                  <Image
                    src="/images/university-logo.png"
                    alt="University Logo"
                    width={60}
                    height={60}
                    className="mx-auto mb-2 h-14 w-auto"
                  />
                  <h2 className="text-xl font-bold text-[#4b2a0a]">
                    Management Information System
                  </h2>
                  <p className="text-xs font-medium text-[#7a4a1d]">
                    Faculty of Engineering, University of Ruhuna
                  </p>
                </div>

                <div className="mb-6 text-center">
                  <h2 className="text-2xl sm:text-3xl font-extrabold text-[#4b2a0a]">
                    Log into your account
                  </h2>
                  <p className="mt-1 text-xs sm:text-sm text-[#7a4a1d] font-medium">
                    Enter your academic credentials to proceed
                  </p>
                </div>

                <form className="space-y-5" onSubmit={handleLogin}>
                  {loginError && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{loginError}</p>}
                  {/* Username */}
                  <div className="space-y-1.5">
                    <label
                      htmlFor="username"
                      className="flex items-center gap-2 text-xs sm:text-sm font-bold text-[#4b2a0a]"
                    >
                      <User size={16} className="text-[#7a4a1d]" />
                      Username :
                    </label>
                    <input
                      id="username"
                      type="text"
                      required
                      autoFocus
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="Enter administrative username"
                      className="h-11 w-full rounded-full border border-[#d99f60] bg-[#f0b37e] px-4 text-sm font-medium text-[#4b2a0a] placeholder:text-[#6b3b14]/70 outline-none transition focus:ring-2 focus:ring-[#4b2a0a]"
                    />
                  </div>

                  {/* Password */}
                  <div className="space-y-1.5">
                    <label
                      htmlFor="password"
                      className="flex items-center gap-2 text-xs sm:text-sm font-bold text-[#4b2a0a]"
                    >
                      <Lock size={16} className="text-[#7a4a1d]" />
                      Password :
                    </label>
                    <input
                      id="password"
                      type="password"
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Enter password"
                      className="h-11 w-full rounded-full border border-[#d99f60] bg-[#f0b37e] px-4 text-sm font-medium text-[#4b2a0a] placeholder:text-[#6b3b14]/70 outline-none transition focus:ring-2 focus:ring-[#4b2a0a]"
                    />
                  </div>

                  {/* Remember me and Forgot password */}
                  <div className="flex items-center justify-between text-xs sm:text-sm pt-1">
                    <label className="flex items-center gap-2 font-medium text-[#4b2a0a] cursor-pointer">
                      <input
                        type="checkbox"
                        checked={rememberMe}
                        onChange={(e) => setRememberMe(e.target.checked)}
                        className="h-4 w-4 rounded accent-[#4b2a0a]"
                      />
                      Remember me
                    </label>

                    <button
                      type="button"
                      onClick={() => alert("Please contact the Faculty of Engineering MIS administrator at ar@eng.ruh.ac.lk to reset credentials.")}
                      className="font-semibold text-[#4b2a0a] hover:text-[#3a2007] hover:underline"
                    >
                      Forgot your password?
                    </button>
                  </div>

                  {/* Submit Button */}
                  <button
                    type="submit"
                    disabled={signingIn}
                    className="flex h-12 w-full items-center justify-center gap-2 rounded-full bg-[#4b2a0a] text-sm sm:text-base font-bold text-white shadow-lg transition duration-200 hover:bg-[#3a2007] hover:shadow-xl active:scale-[0.98]"
                  >
                    <span>{signingIn ? "Signing in..." : "Login"}</span>
                    <ArrowRight size={18} />
                  </button>
                </form>

                {/* Footer notes */}
                <div className="mt-6 border-t border-[#d9c59f] pt-4 text-center text-xs text-[#7a4a1d]">
                  <p className="font-semibold">
                    Timetable Management System
                  </p>
                  <p className="mt-0.5 text-[11px] text-[#8a5d33]">
                    © 2026 Faculty of Engineering, University of Ruhuna
                  </p>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <section className="w-full max-w-3xl rounded-3xl border-4 border-[#f5c04a] bg-[#f4e7c8] p-7 shadow-2xl sm:p-10">
            <div className="mb-6 flex items-center gap-3 border-b border-[#d9c59f] pb-5">
              <Info size={28} className="text-[#7a4a1d]" />
              <div>
                <h2 className="text-2xl font-extrabold text-[#4b2a0a]">About MIS & Faculty</h2>
                <p className="mt-1 text-sm text-[#7a4a1d]">Faculty of Engineering, University of Ruhuna</p>
              </div>
            </div>
            <p className="text-sm leading-relaxed text-[#7a4a1d]">
              The Management Information System supports academic and administrative activities at the Faculty of Engineering, University of Ruhuna. The Timetable Management System helps manage lecture and examination scheduling.
            </p>
            <div className="mt-6 space-y-4 rounded-2xl border border-[#d9c59f] bg-white/40 p-5 text-sm text-[#4b2a0a]">
              <p className="flex items-center gap-3"><MapPin size={20} className="shrink-0 text-[#7a4a1d]" />Faculty of Engineering, Hapugala, Galle, Sri Lanka</p>
              <p className="flex items-center gap-3"><Phone size={20} className="shrink-0 text-[#7a4a1d]" /><a className="underline" href="mailto:ar@eng.ruh.ac.lk">Contact the Faculty MIS administrator</a></p>
            </div>
            <button type="button" onClick={() => setActiveTab("login")} className="mt-6 inline-flex items-center gap-2 rounded-full bg-[#4b2a0a] px-6 py-3 text-sm font-bold text-white hover:bg-[#3a2007]">Back to Login <ArrowRight size={18} /></button>
          </section>
        )}
      </div>
    </main>
  );
}
