"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { User, Lock, ArrowRight, Info, MapPin, Phone, ShieldCheck } from "lucide-react";

const activeNav = "rounded-full bg-[#f5c04a] px-[18px] py-1.5 text-[#4b2a0a] shadow-sm";
const inactiveNav = "rounded-full px-4 py-1.5 text-slate-700 transition hover:bg-[#f4e7c8]";
const fieldLabel = "flex items-center gap-2 text-[13px] leading-[19px] font-bold text-[#4b2a0a]";
const credentialInput = "h-[42px] w-full rounded-full border border-[#e2d4b7] bg-[#e8effb] px-3.5 text-sm text-slate-900 placeholder:text-slate-500 focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[#7a4a1d]";

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
    <main className="flex min-h-screen flex-col bg-[#faf8f0] font-sans">
      <header className="flex min-h-[73px] flex-wrap items-center justify-between gap-3 border-b border-[#efe7d3] bg-[#faf6ed] px-5 py-3 sm:px-10">
        <div className="flex items-center gap-2.5">
          <Image
            src="/images/university-logo.png"
            alt=""
            width={30}
            height={42}
            className="h-[42px] w-auto"
            preload
          />
          <div>
            <p className="text-[17px] leading-[22px] font-bold text-[#4b2a0a]">Faculty of Engineering</p>
            <p className="mt-0.5 text-[11px] leading-4 text-[#526277]">University of Ruhuna, Sri Lanka</p>
          </div>
        </div>

        <nav aria-label="Main navigation" className="flex items-center gap-2 text-[13px] font-semibold">
          <button
            type="button"
            onClick={() => setActiveTab("login")}
            aria-current={activeTab === "login" ? "page" : undefined}
            className={activeTab === "login" ? activeNav : inactiveNav}
          >
            Login
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("about")}
            aria-current={activeTab === "about" ? "page" : undefined}
            className={activeTab === "about" ? activeNav : inactiveNav}
          >
            About the System
          </button>
        </nav>
      </header>

      <div className="flex flex-1 items-center justify-center px-4 pt-7 pb-6 sm:px-8">
        {activeTab === "login" ? (
          <div className="grid w-full max-w-[880px] items-center gap-8 md:grid-cols-2 md:gap-12 lg:gap-20">
            <div className="flex flex-col items-center text-center">
              <Image
                src="/images/university-logo.png"
                alt="University of Ruhuna"
                width={112}
                height={155}
                className="h-[84px] w-auto md:h-[155px]"
                preload
              />
              <p className="mt-4 text-[11px] leading-4 font-bold tracking-[0.14em] text-[#7a4a1d] uppercase">
                Academic Scheduling
              </p>
              <h1 className="mt-2 text-[27px] leading-9 font-bold tracking-[-0.015em] text-[#4b2a0a]">
                Timetable Management <br className="hidden md:block" />System
              </h1>
              <div className="mt-4 mb-[17px] hidden h-1 w-12 rounded-full bg-[#f5c04a] md:block" />
              <p className="mt-2 text-[17px] leading-6 font-bold text-slate-800 md:mt-0">Faculty of Engineering</p>
              <p className="mt-1.5 text-[13px] leading-5 text-[#526277]">University of Ruhuna, Hapugala, Galle</p>
              <p className="mt-[14px] hidden items-center gap-2 text-[11px] leading-[17px] text-[#526277] md:flex">
                <ShieldCheck size={14} className="text-[#d5a639]" aria-hidden="true" />
                Lecture and examination scheduling
              </p>
            </div>

            <section aria-labelledby="login-heading" className="mx-auto w-full max-w-[400px] rounded-[15px] border border-[#eadfc2] bg-[#faf6ed] px-5 py-[34px] sm:px-7 shadow-[0_5px_14px_rgba(83,54,13,0.04)]">
              <div className="text-center">
                <h2 id="login-heading" className="text-[22px] leading-7 font-bold text-[#4b2a0a]">
                  Log into your account
                </h2>
                <p className="mt-[7px] text-[13px] leading-5 text-[#7a4a1d]">
                  Enter your academic credentials to proceed
                </p>
              </div>

              <form className="mt-5 space-y-[15px]" onSubmit={handleLogin}>
                {loginError && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{loginError}</p>}
                <div className="space-y-[5px]">
                  <label htmlFor="username" className={fieldLabel}>
                    <User size={14} className="text-[#7a4a1d]" aria-hidden="true" />
                    Username :
                  </label>
                  <input
                    id="username"
                    name="username"
                    type="text"
                    required
                    autoComplete="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="Enter administrative username"
                    className={credentialInput}
                  />
                </div>

                <div className="space-y-[5px]">
                  <label htmlFor="password" className={fieldLabel}>
                    <Lock size={14} className="text-[#7a4a1d]" aria-hidden="true" />
                    Password :
                  </label>
                  <input
                    id="password"
                    name="password"
                    type="password"
                    required
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter password"
                    className={credentialInput}
                  />
                </div>

                <div className="flex min-h-[23px] flex-wrap items-center justify-between gap-2 pt-0.5 text-[11px] text-[#4b2a0a]">
                  <label className="flex cursor-pointer items-center gap-1.5">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      className="h-4 w-4 accent-[#4b2a0a]"
                    />
                    Remember me
                  </label>

                  <button
                    type="button"
                    onClick={() => alert("Please contact the Faculty of Engineering MIS administrator at ar@eng.ruh.ac.lk to reset credentials.")}
                    className="font-semibold hover:underline"
                  >
                    Forgot your password?
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={signingIn}
                  className="flex h-11 w-full items-center justify-center gap-2 rounded-full bg-[#4b2a0a] text-[15px] font-bold text-white shadow-md transition hover:bg-[#3a2007] disabled:opacity-75"
                >
                  {signingIn ? "Signing in..." : "Login"}
                  <ArrowRight size={15} aria-hidden="true" />
                </button>
              </form>

              <footer className="mt-5 border-t border-[#e8dcbf] pt-3 text-center text-[11px] leading-4 text-[#7a4a1d]">
                <p className="font-semibold">Timetable Management System</p>
                <p>© 2026 Faculty of Engineering, University of Ruhuna</p>
              </footer>
            </section>
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
